(function(){
var currentWarehouseId=null;

function escapeHtml(s){return String(s||'').replace(/[&<>"']/g,function(m){return{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]})}

function nativeGet(url){
  return new Promise(function(resolve,reject){
    if(window.NativeHttp&&typeof window.NativeHttp.httpGet==='function'){
      try{
        var result=window.NativeHttp.httpGet(url);
        if(result&&result.indexOf('__ERROR__')===0){
          reject(new Error(result.substring(9)));
        }else if(result){
          resolve(result);
        }else{
          reject(new Error('空响应'));
        }
      }catch(e){reject(e)}
    }else{
      var xhr=new XMLHttpRequest();
      xhr.open('GET',url,true);
      xhr.responseType='text';
      xhr.timeout=15000;
      xhr.onload=function(){
        if(xhr.status>=200&&xhr.status<300){
          var text=xhr.responseText;
          if(text){resolve(text)}else{reject(new Error('空响应'))}
        }else{reject(new Error('HTTP '+xhr.status))}
      };
      xhr.onerror=function(){reject(new Error('网络错误'))};
      xhr.ontimeout=function(){reject(new Error('请求超时'))};
      xhr.send();
    }
  });
}

function parseTvboxConfig(text){
  if(!text||!text.trim()) return null;
  text=text.trim();
  // 尝试直接解析 JSON
  try{
    var config=JSON.parse(text);
    if(config&&Array.isArray(config.sites)&&config.sites.length){
      return{sites:config.sites,lives:config.lives||[],parses:config.parses||[],spider:config.spider||null,flags:config.flags||[],rules:config.rules||[]};
    }
    // 可能是 TVBox 风格的 {code:1, list:[...]} 格式
    if(config&&config.code===1&&Array.isArray(config.list)){
      return{sites:config.list.map(function(v){return{name:v.vod_name||'',type:0,api:v.vod_id||''}}),lives:[],parses:[],spider:null,flags:[],rules:[]};
    }
    // 有 sites 但不是数组
    if(config&&config.sites&&!Array.isArray(config.sites)){
      return{sites:[],lives:[],parses:[],spider:null,flags:[],rules:[]};
    }
    return null;
  }catch(e){
    // 尝试清理 BOM 和不可见字符后再次解析
    try{
      text=text.replace(/^\uFEFF/,'').replace(/\u0000/g,'');
      var config2=JSON.parse(text);
      if(config2&&Array.isArray(config2.sites)&&config2.sites.length){
        return{sites:config2.sites,lives:config2.lives||[],parses:config2.parses||[],spider:config2.spider||null,flags:config2.flags||[],rules:config2.rules||[]};
      }
      return null;
    }catch(e2){
      // 尝试提取 JSON 对象
      var jsonMatch=text.match(/\{[\s\S]*\}/);
      if(jsonMatch){
        try{
          var config3=JSON.parse(jsonMatch[0]);
          if(config3&&Array.isArray(config3.sites)&&config3.sites.length){
            return{sites:config3.sites,lives:config3.lives||[],parses:config3.parses||[],spider:config3.spider||null,flags:config3.flags||[],rules:config3.rules||[]};
          }
        }catch(e3){}
      }
      return null;
    }
  }
}

function extractJsonFromImage(text){
  if(!text||!text.trim()) return null;
  // JPEG 格式检测：包含 JFIF 或 APP1 段
  if(text.indexOf('JFIF')>=0||text.indexOf('�')>=0){
    // 尝试提取 Base64 编码的 JSON
    // 饭太硬格式：JPEG 图片中包含 base64 编码的配置
    var base64Match=text.match(/([A-Za-z0-9+\/]{50,}={0,2})/g);
    if(base64Match){
      for(var i=0;i<base64Match.length;i++){
        var b64=base64Match[i];
        if(b64.length>500){
          try{
            var decoded=atob(b64);
            var config=JSON.parse(decoded);
            if(config&&Array.isArray(config.sites)&&config.sites.length){
              return config;
            }
          }catch(e){}
        }
      }
    }
    // 尝试从二进制数据中提取 JSON 对象
    var jsonMatch=text.match(/\{[^{}]*"sites"\s*:\s*\[[\s\S]*?\]\s*[^{}]*\}/);
    if(jsonMatch){
      try{
        return JSON.parse(jsonMatch[0]);
      }catch(e){}
    }
  }
  // BMP/PNG 等其他图片格式：尝试直接提取 JSON
  var jsonMatch2=text.match(/\{[\s\S]*"sites"\s*:\s*\[[\s\S]*?\][\s\S]*\}/);
  if(jsonMatch2){
    try{
      return JSON.parse(jsonMatch2[0]);
    }catch(e){}
  }
  return null;
}

function fetchAndSaveWarehouse(warehouseId,url){
  return nativeGet(url).then(function(text){
    var config=parseTvboxConfig(text);
    // 如果不是标准 JSON，尝试从图片中提取
    if(!config){
      config=extractJsonFromImage(text);
    }
    if(!config){throw new Error('配置格式错误，无法解析JSON')}
    if(!config.sites||!config.sites.length){throw new Error('配置中没有找到站点数据')}
    var savePromises=config.sites.map(function(site){
      return NCDB.saveSiteConfig(warehouseId,{
        key:site.key||site.name||'',
        name:site.name||'',
        type:typeof site.type==='number'?site.type:(site.type==='3'?3:parseInt(site.type)||0),
        api:site.api||'',
        searchable:site.searchable||0,
        quickSearch:site.quickSearch||0,
        filterable:site.filterable||0,
        type_flag:site.type_flag||0,
        playerType:site.playerType||0,
        ext:site.ext?(typeof site.ext==='string'?site.ext:JSON.stringify(site.ext)):'{}',
        pass:site.pass||false
      });
    });
    if(config.lives&&config.lives.length&&typeof NCDB.saveLiveChannels==='function'){
      var liveChannels=config.lives.map(function(live){
        return{name:live.name||'',url:live.url||'',group:live.group||'直播',logo:live.logo||''};
      });
      savePromises.push(NCDB.saveLiveChannels('warehouse_'+warehouseId,liveChannels));
    }
    return Promise.all(savePromises).then(function(){
      return config.sites.length;
    });
  });
}

window.showRepoPanel=function(){
  var overlay=document.getElementById('repoPanelOverlay');
  if(overlay){overlay.style.display='flex';overlay.classList.add('show')}
  refreshWarehouses();
};

window.hideRepoPanel=function(){
  var overlay=document.getElementById('repoPanelOverlay');
  if(overlay){overlay.style.display='none';overlay.classList.remove('show')}
};

window.refreshWarehouses=function(){
  NCDB.getAllWarehouses().then(function(warehouses){
    renderWarehouseList(warehouses);
    if(warehouses.length){
      if(!currentWarehouseId||!warehouses.find(function(w){return w.id===currentWarehouseId})){
        currentWarehouseId=warehouses[0].id;
      }
      renderWarehouseSites(currentWarehouseId);
    }else{
      currentWarehouseId=null;
      renderEmptyWarehouse();
    }
  });
};

function renderWarehouseList(warehouses){
  var sidebar=document.getElementById('repoSidebar');
  if(!warehouses.length){
    sidebar.innerHTML='<div style="padding:20px;text-align:center;color:#667788;font-size:13px">暂无仓库，请添加</div>';
    return;
  }
  sidebar.innerHTML=warehouses.map(function(w){
    var active=currentWarehouseId===w.id?'repo-sidebar-item active':'repo-sidebar-item';
    return '<div class="'+active+'" onclick="selectWarehouse('+w.id+')">'+escapeHtml(w.name||'未命名')+'</div>';
  }).join('');
}

window.selectWarehouse=function(id){
  currentWarehouseId=id;
  NCDB.getAllWarehouses().then(function(warehouses){
    renderWarehouseList(warehouses);
    renderWarehouseSites(id);
  });
};

function renderWarehouseSites(warehouseId){
  var content=document.getElementById('repoContent');
  NCDB.getAllSites().then(function(allSites){
    var sites=allSites.filter(function(s){return s.warehouseId===warehouseId});
    if(!sites.length){
      content.innerHTML='<div style="padding:40px;text-align:center;color:#667788"><div style="font-size:32px;margin-bottom:12px">📦</div><div>该仓库暂无站点</div><div style="font-size:12px;margin-top:8px">点击右上角 + 添加仓库地址</div></div>';
      return;
    }
    content.innerHTML='<div class="repo-site-grid">'+sites.map(function(s){
      var typeLabel=s.type==0?'XML':s.type==1?'JSON':s.type==3?'JS插件':'未知';
      var searchTag=(s.searchable||s.quickSearch)?' · 可搜索':'';
      var usableTag=s.type===3?'<span style="color:#ff6b6b;font-size:10px;margin-left:4px">[插件]</span>':'<span style="color:#4ade80;font-size:10px;margin-left:4px">[可用]</span>';
      return '<div class="repo-site-item" onclick="selectSiteById('+s.id+');hideRepoPanel()">'+
        '<div class="repo-site-icon">'+(s.searchable?'🔍':'📺')+'</div>'+
        '<div class="repo-site-info">'+
          '<span class="repo-site-name">'+escapeHtml(s.name)+usableTag+'</span>'+
          '<span class="repo-site-desc">'+typeLabel+searchTag+'</span>'+
        '</div>'+
      '</div>';
    }).join('')+'</div>';
  });
}

function renderEmptyWarehouse(){
  var content=document.getElementById('repoContent');
  content.innerHTML='<div style="padding:40px;text-align:center;color:#667788"><div style="font-size:32px;margin-bottom:12px">📦</div><div>暂无仓库</div><div style="font-size:12px;margin-top:8px">点击右上角 + 添加仓库地址</div></div>';
}

window.selectSiteById=function(id){
  if(window.selectSite){
    window.selectSite(id);
  }
};

window.showAddWarehouseDialog=function(){
  var overlay=document.getElementById('addRepoDialogOverlay');
  if(overlay){overlay.style.display='flex';overlay.classList.add('show')}
  var statusEl=document.getElementById('addRepoStatus');
  if(statusEl){statusEl.style.display='none';statusEl.textContent=''}
  var btn=document.getElementById('addRepoConfirmBtn');
  if(btn){btn.disabled=false;btn.textContent='确定添加'}
};

window.hideAddRepoDialog=function(){
  var overlay=document.getElementById('addRepoDialogOverlay');
  if(overlay){overlay.style.display='none';overlay.classList.remove('show')}
};

window.confirmAddRepo=function(){
  var nameInput=document.getElementById('addRepoName');
  var urlInput=document.getElementById('addRepoUrl');
  var btn=document.getElementById('addRepoConfirmBtn');
  var statusEl=document.getElementById('addRepoStatus');
  var name=nameInput?nameInput.value.trim():'';
  var url=urlInput?urlInput.value.trim():'';
  if(!name){if(statusEl){statusEl.style.display='block';statusEl.style.color='#ff6b6b';statusEl.textContent='请输入仓库名称'}return}
  if(!url){if(statusEl){statusEl.style.display='block';statusEl.style.color='#ff6b6b';statusEl.textContent='请输入仓库地址'}return}
  if(!/^https?:\/\//i.test(url)){if(statusEl){statusEl.style.display='block';statusEl.style.color='#ff6b6b';statusEl.textContent='地址必须以 http:// 或 https:// 开头'}return}
  if(btn){btn.disabled=true;btn.textContent='正在获取...'}
  if(statusEl){statusEl.style.display='block';statusEl.style.color='#8899aa';statusEl.textContent='正在连接仓库地址...'}
  NCDB.saveWarehouse(name,url).then(function(warehouseId){
    if(statusEl){statusEl.textContent='正在解析配置...'}
    return fetchAndSaveWarehouse(warehouseId,url).then(function(count){
      if(statusEl){statusEl.style.color='#4ade80';statusEl.textContent='成功！已加载 '+count+' 个站点'}
      setTimeout(function(){
        hideAddRepoDialog();
        refreshWarehouses();
        if(btn){btn.disabled=false;btn.textContent='确定添加'}
      },800);
    });
  }).catch(function(e){
    if(statusEl){statusEl.style.color='#ff6b6b';statusEl.textContent='获取失败：'+e.message}
    if(btn){btn.disabled=false;btn.textContent='确定添加'}
  });
};

window.importConfig=function(){
  var input=document.getElementById('importFileInput');
  if(!input||!input.files||!input.files[0]){alert('请选择文件');return}
  var reader=new FileReader();
  reader.onload=function(e){
    try{
      var config=JSON.parse(e.target.result);
      var sites=config.sites||[];
      var promises=sites.map(function(site){
        return NCDB.saveSiteConfig(null,{
          key:site.key||'',name:site.name||'',type:site.type||0,
          api:site.api||'',searchable:site.searchable||0,quickSearch:site.quickSearch||0,
          filterable:site.filterable||0,type_flag:site.type_flag||0,
          playerType:site.playerType||0,ext:site.ext?JSON.stringify(site.ext):'{}',
          pass:site.pass||false,sourceType:'local'
        });
      });
      Promise.all(promises).then(function(){
        alert('导入成功，共 '+sites.length+' 个站点');
        refreshWarehouses();
      }).catch(function(err){alert('导入失败：'+err.message)});
    }catch(ex){alert('JSON格式错误')}
  };
  reader.readAsText(input.files[0]);
};

window.exportConfig=function(){
  NCDB.getAllSites().then(function(sites){
    var config={sites:sites.map(function(s){
      return{key:s.key,name:s.name,type:s.type,api:s.api,searchable:s.searchable,
             quickSearch:s.quickSearch,filterable:s.filterable,ext:s.ext?JSON.parse(s.ext):{}};
    })};
    var blob=new Blob([JSON.stringify(config,null,2)],{type:'application/json'});
    var url=URL.createObjectURL(blob);
    var a=document.createElement('a');
    a.href=url;a.download='tvbox-config.json';a.click();
    URL.revokeObjectURL(url);
  });
};
})();
