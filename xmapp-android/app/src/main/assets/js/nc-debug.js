// Debug Log Panel - toggle button + collapsible panel + draggable + copy
(function(){
  var _orig = {};
  var _levels = ['log','info','warn','error','debug'];
  for (var i = 0; i < _levels.length; i++) {
    var l = _levels[i];
    if (console[l]) _orig[l] = console[l];
  }

  for (var j = 0; j < _levels.length; j++) {
    (function(k) {
      console[k] = function() {
        var args = [];
        for (var i = 0; i < arguments.length; i++) {
          try {
            args.push(typeof arguments[i] === 'object' ? JSON.stringify(arguments[i]) : String(arguments[i]));
          } catch(e) {
            args.push(String(arguments[i]));
          }
        }
        var msg = args.join(' ');
        var ts = new Date().toLocaleTimeString();
        
        var body = document.getElementById('dbgBody');
        if (body) {
          var color = '#aab';
          if (k === 'error') color = '#f66';
          else if (k === 'warn') color = '#fa0';
          else if (k === 'info') color = '#6af';
          else if (k === 'debug') color = '#a6f';
          
          var div = document.createElement('div');
          div.style.color = color;
          div.style.padding = '3px 0';
          div.style.borderBottom = '1px solid rgba(80,80,160,.1)';
          div.innerHTML = '<span style="color:#556">[' + ts + ']</span> ' + msg;
          body.appendChild(div);
          
          while (body.children.length > 500) {
            body.removeChild(body.firstChild);
          }
        }
        
        try { _orig[k].apply(console, arguments); } catch(e) {}
      };
    })(_levels[j]);
  }

  var panel = document.getElementById('dbgPanel');
  var toggleBtn = document.getElementById('dbgToggleBtn');
  var hideBtn = document.getElementById('dbgHide');
  var _isPanelOpen = false;

  function showPanel() {
    if (panel) panel.style.display = 'block';
    _isPanelOpen = true;
    console.log('[DBG] Panel shown');
  }

  function hidePanel() {
    if (panel) panel.style.display = 'none';
    _isPanelOpen = false;
    console.log('[DBG] Panel hidden');
  }

  // --- Toggle button: tap opens panel, drag moves button ---
  if (toggleBtn) {
    // Start with panel hidden, button visible
    hidePanel();

    var _btnDownX = 0, _btnDownY = 0, _btnMoved = false;

    toggleBtn.addEventListener('touchstart', function(e) {
      var t = e.touches[0];
      _btnDownX = t.clientX;
      _btnDownY = t.clientY;
      _btnMoved = false;
    }, { passive: true });

    toggleBtn.addEventListener('touchend', function(e) {
      var t = e.changedTouches[0];
      var dx = Math.abs(t.clientX - _btnDownX);
      var dy = Math.abs(t.clientY - _btnDownY);
      if (dx > 8 || dy > 8) {
        _btnMoved = true;
      }
      if (!_btnMoved && !_isPanelOpen) {
        showPanel();
      }
    }, { passive: true });

    toggleBtn.addEventListener('mousedown', function(e) {
      _btnDownX = e.clientX;
      _btnDownY = e.clientY;
      _btnMoved = false;
    });

    toggleBtn.addEventListener('mouseup', function(e) {
      var dx = Math.abs(e.clientX - _btnDownX);
      var dy = Math.abs(e.clientY - _btnDownY);
      if (dx > 8 || dy > 8) {
        _btnMoved = true;
      }
      if (!_btnMoved && !_isPanelOpen) {
        showPanel();
      }
    });
  }

  // Hide button inside panel
  if (hideBtn) {
    hideBtn.addEventListener('click', function(e) {
      e.stopPropagation();
      hidePanel();
    });
  }

  // Clear logs button
  var clearBtn = document.getElementById('dbgClear');
  if (clearBtn) {
    clearBtn.addEventListener('click', function(e) {
      e.stopPropagation();
      var body = document.getElementById('dbgBody');
      if (body) body.innerHTML = '';
    });
  }

  // Copy logs button
  var copyBtn = document.getElementById('dbgCopy');
  if (copyBtn) {
    copyBtn.addEventListener('click', function(e) {
      e.stopPropagation();
      var body = document.getElementById('dbgBody');
      if (!body) return;
      var text = '';
      for (var i = 0; i < body.children.length; i++) {
        text += body.children[i].textContent + '\n';
      }
      if (!text) text = '(空)';
      
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(function() {
          copyBtn.textContent = '已复制';
          setTimeout(function(){ copyBtn.textContent = '复制'; }, 1500);
        }).catch(function() {
          fallbackCopy(text);
        });
      } else {
        fallbackCopy(text);
      }
    });
  }

  function fallbackCopy(text) {
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    try { 
      document.execCommand('copy'); 
      copyBtn.textContent = '已复制'; 
      setTimeout(function(){ copyBtn.textContent = '复制'; }, 1500); 
    }
    catch(e) { alert('复制失败'); }
    document.body.removeChild(ta);
  }

  // --- Draggable panel via header ---
  var isDragging = false;
  var dragStartX, dragStartY, panelOrigLeft, panelOrigTop;

  var header = document.getElementById('dbgHeader');
  if (header && panel) {
    panel.style.left = '';
    panel.style.right = '12px';
    panel.style.bottom = '130px';
    panel.style.top = 'auto';

    function onPointerDown(clientX, clientY) {
      isDragging = true;
      dragStartX = clientX;
      dragStartY = clientY;
      panelOrigLeft = panel.offsetLeft;
      panelOrigTop = panel.offsetTop;
      panel.style.transition = 'none';
      panel.style.right = 'auto';
      panel.style.bottom = 'auto';
    }

    function onPointerMove(clientX, clientY) {
      if (!isDragging) return;
      var dx = clientX - dragStartX;
      var dy = clientY - dragStartY;
      panel.style.left = (panelOrigLeft + dx) + 'px';
      panel.style.top = (panelOrigTop + dy) + 'px';
    }

    function onPointerUp() {
      if (isDragging) {
        isDragging = false;
        if (panel) panel.style.transition = '';
      }
    }

    header.addEventListener('mousedown', function(e) {
      onPointerDown(e.clientX, e.clientY);
      e.preventDefault();
    });
    document.addEventListener('mousemove', function(e) { onPointerMove(e.clientX, e.clientY); });
    document.addEventListener('mouseup', onPointerUp);

    header.addEventListener('touchstart', function(e) {
      var t = e.touches[0];
      onPointerDown(t.clientX, t.clientY);
    }, { passive: true });
    document.addEventListener('touchmove', function(e) {
      if (!isDragging) return;
      var t = e.touches[0];
      onPointerMove(t.clientX, t.clientY);
    }, { passive: true });
    document.addEventListener('touchend', onPointerUp);
  }

  console.log('[DBG] Panel initialized (toggle button + collapsible)');
})();
