// 日志浮窗 - 纯 H5 方案，不影响原有 console
(function(){
  var _origConsole = {};
  var _logBuffer = [];
  var _maxLines = 500;
  var _filterLevel = 'all'; // 'all' | 'log' | 'warn' | 'error'
  var _visible = false;

  // Preserve original console methods
  var levels = ['log','info','warn','error','debug'];
  for (var i = 0; i < levels.length; i++) {
    var lvl = levels[i];
    if (console[lvl]) {
      _origConsole[lvl] = console[lvl];
    }
  }

  // Override console methods
  function overrideConsole(level, method) {
    var argsToString = function(args) {
      var parts = [];
      for (var i = 0; i < args.length; i++) {
        var a = args[i];
        if (typeof a === 'object') {
          try { parts.push(JSON.stringify(a)); } catch(e) { parts.push(String(a)); }
        } else {
          parts.push(String(a));
        }
      }
      return parts.join(' ');
    };

    console[level] = function() {
      var msg = argsToString(arguments);
      var ts = new Date().toLocaleTimeString();
      _logBuffer.unshift({ level: level, msg: msg, ts: ts });
      if (_logBuffer.length > _maxLines) _logBuffer.pop();
      if (_origConsole[level]) {
        try { _origConsole[level].apply(console, arguments); } catch(e) {}
      }
      if (_visible) renderLogs();
    };
  }

  for (var j = 0; j < levels.length; j++) {
    overrideConsole(levels[j], levels[j]);
  }

  // Wait for DOM ready then create panel
  function createPanel() {
    if (document.getElementById('debugLogPanel')) return; // Already created
    
    var panel = document.createElement('div');
    panel.id = 'debugLogPanel';
    panel.innerHTML = '' +
      '<div id="debugLogHeader" style="display:flex;justify-content:space-between;align-items:center;padding:6px 10px;background:#1a1a2e;border-bottom:1px solid #333;cursor:move;user-select:none">' +
        '<span style="color:#0f0;font-size:12px;font-weight:bold">DEBUG LOG</span>' +
        '<div style="display:flex;gap:6px;align-items:center">' +
          '<select id="debugFilter" style="background:#222;color:#ccc;border:1px solid #444;border-radius:3px;font-size:11px;padding:2px">' +
            '<option value="all">ALL</option>' +
            '<option value="log">LOG</option>' +
            '<option value="warn">WARN</option>' +
            '<option value="error">ERROR</option>' +
          '</select>' +
          '<button id="dbgClearBtn" style="background:#333;color:#ccc;border:1px solid #555;border-radius:3px;font-size:11px;padding:2px 6px;cursor:pointer">CLEAR</button>' +
          '<button id="dbgCloseBtn" style="background:#c33;color:#fff;border:none;border-radius:3px;font-size:11px;padding:2px 6px;cursor:pointer">X</button>' +
        '</div>' +
      '</div>' +
      '<div id="debugLogBody" style="max-height:200px;overflow-y:auto;padding:4px 8px;font-family:monospace;font-size:10px;line-height:1.4;background:#000"></div>';

    document.body.appendChild(panel);

    // Bind events without inline onclick
    (function bindDebugEvents(){
      var clearBtn=document.getElementById('dbgClearBtn');
      if(clearBtn)clearBtn.addEventListener('click',function(){window._dbgClear()});
      var closeBtn=document.getElementById('dbgCloseBtn');
      if(closeBtn)closeBtn.addEventListener('click',function(){window._dbgToggle()});
      var filterSel=document.getElementById('debugFilter');
      if(filterSel)filterSel.addEventListener('change',function(){window._dbgFilter(filterSel.value)});
    })();

    // Initial position - top left corner
    panel.style.position = 'fixed';
    panel.style.left = '5px';
    panel.style.top = '5px';
    panel.style.zIndex = '2147483647';
    panel.style.width = '300px';
    panel.style.borderRadius = '4px';
    panel.style.overflow = 'hidden';
    panel.style.boxShadow = '0 2px 10px rgba(0,0,0,0.8)';

    // Drag support
    (function(){
      var header = document.getElementById('debugLogHeader');
      var offsetX, offsetY, startX, startY;
      header.addEventListener('mousedown', function(e){
        startX = e.clientX; startY = e.clientY;
        offsetX = panel.offsetLeft; offsetY = panel.offsetTop;
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
      });
      header.addEventListener('touchstart', function(e){
        var t = e.touches[0];
        startX = t.clientX; startY = t.clientY;
        offsetX = panel.offsetLeft; offsetY = panel.offsetTop;
        document.addEventListener('touchmove', onMove);
        document.addEventListener('touchend', onUp);
      });
      function onMove(e){
        var x = (e.touches ? e.touches[0].clientX : e.clientX);
        var y = (e.touches ? e.touches[0].clientY : e.clientY);
        panel.style.position = 'fixed';
        panel.style.left = (offsetX + x - startX) + 'px';
        panel.style.top = (offsetY + y - startY) + 'px';
      }
      function onUp(){
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
        document.removeEventListener('touchmove', onMove);
        document.removeEventListener('touchend', onUp);
      }
    })();

    // Auto-hide after 30 seconds in production
    // setTimeout(function(){ panel.style.display='none'; _visible=false; }, 30000);
  }

  // Try multiple approaches to ensure panel is created
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', createPanel);
  }
  // Also try immediately
  if (document.body) {
    createPanel();
  } else {
    // Body not ready yet, wait a bit
    setTimeout(createPanel, 1000);
    setTimeout(createPanel, 3000);
  }

  // Toggle visibility
  window._dbgToggle = function(){
    _visible = !_visible;
    var p = document.getElementById('debugLogPanel');
    if (p) {
      p.style.display = _visible ? 'block' : 'none';
    } else if (_visible) {
      // Panel DOM was removed, recreate it
      createPanel();
    }
  };

  // Filter
  window._dbgFilter = function(val){
    _filterLevel = val;
    renderLogs();
  };

  // Clear
  window._dbgClear = function(){
    _logBuffer = [];
    renderLogs();
  };

  // Render
  function renderLogs(){
    var body = document.getElementById('debugLogBody');
    if (!body) return;
    var filtered = _filterLevel === 'all' ? _logBuffer : _logBuffer.filter(function(l){ return l.level === _filterLevel; });
    var html = '';
    for (var i = 0; i < filtered.length; i++) {
      var c = '#ccc';
      if (filtered[i].level === 'error') c = '#f44';
      else if (filtered[i].level === 'warn') c = '#fa0';
      else if (filtered[i].level === 'debug') c = '#88f';
      html += '<div style="color:' + c + '"><span style="color:#555">[' + filtered[i].ts + ']</span> ' + escHtml(filtered[i].msg) + '</div>';
    }
    body.innerHTML = html || '<div style="color:#555">No logs</div>';
  }

  function escHtml(s){
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  // Show panel by default
  _visible = true;
  renderLogs();

})();
