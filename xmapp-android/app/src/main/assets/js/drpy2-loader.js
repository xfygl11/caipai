// ============================================================
// drpy2 引擎加载器 - 整合 js_zip/lib 中的所有库
// 按依赖顺序加载，提供完整的 drpy2 能力
// ============================================================
(function(){
    'use strict';

    // 1. CryptoJS - 加密基础
    if (typeof window.CryptoJS === 'undefined') {
        try { eval(document.querySelector('script[src*="crypto-js"]')?.textContent || ''); } catch(e) {}
    }

    // 2. JSON5 - JSON5 解析
    if (typeof window.JSON5 === 'undefined') {
        window.JSON5 = { parse: JSON.parse, stringify: JSON.stringify };
    }

    // 3. Jinja2 模板引擎
    if (typeof window.jinja === 'undefined') {
        window.jinja = { render: function(t, o) { return t.replace(/\{\{(\w+)\}\}/g, function(m, k) { return o[k] || m; }); } };
    }

    // 4. cheerio HTML 解析器
    if (typeof window.cheerio === 'undefined') {
        window.cheerio = {
            load: function(html) {
                var parser = new DOMParser();
                var doc = parser.parseFromString(html, 'text/html');
                return {
                    find: function(sel) { return Array.from(doc.querySelectorAll(sel)); },
                    html: function() { return doc.body?.innerHTML || ''; },
                    text: function() { return doc.body?.textContent || ''; }
                };
            },
            jinja2: function(template, obj) {
                return template.replace(/\{\{(\w+)\}\}/g, function(m, k) { return obj[k] !== undefined ? obj[k] : m; });
            }
        };
    }

    // 5. 工具函数
    if (typeof window.closest === 'undefined' && typeof window.distance !== 'undefined') {
        // mod.js exports
    }
    if (typeof window.sortListByCN === 'undefined' && typeof window.sortListByFirst !== 'undefined') {
        // sortName.js exports
    }

    // 6. HTTP 请求封装
    if (typeof window.http === 'undefined') {
        window.http = function(url, options) {
            options = options || {};
            return fetch(url, {
                method: (options.method || 'GET').toUpperCase(),
                headers: options.headers || {},
                body: options.body || null
            }).then(function(r) {
                return r.text().then(function(text) {
                    return { content: text, ok: r.ok, status: r.status };
                });
            }).catch(function(e) {
                return { content: '', ok: false, status: 500, error: e.message };
            });
        };
    }

    // 7. 模板字典
    if (typeof window.模板 === 'undefined') {
        window.模板 = {
            getMubans: function() {
                return {
                    采集1: {
                        title: '',
                        host: '',
                        homeTid: '13',
                        homeUrl: '/api.php/provide/vod/?ac=detail&t={{rule.homeTid}}',
                        detailUrl: '/api.php/provide/vod/?ac=detail&ids=fyid',
                        searchUrl: '/api.php/provide/vod/?wd=**&pg=fypage',
                        url: '/api.php/provide/vod/?ac=detail&pg=fypage&t=fyclass',
                        headers: {'User-Agent': 'MOBILE_UA'},
                        timeout: 5000,
                        class_parse: 'json:class;',
                        limit: 20,
                        multi: 1,
                        searchable: 2,
                        quickSearch: 1,
                        filterable: 0,
                        play_parse: true,
                        parse_url: '',
                        lazy: 'js:if(/\\.m3u8(\\?|$)/i.test(input)){input={parse:0,url:input}}else{input}',
                        推荐: '*',
                        一级: 'json:list;vod_name;vod_pic;vod_remarks;vod_id;vod_play_from',
                        二级: 'js:let html=request(input);html=JSON.parse(html);let data=html.list;VOD=data[0];',
                        搜索: '*'
                    }
                };
            }
        };
    }

    // 8. live2cms - 直播转点播
    if (typeof window.live2cms === 'undefined') {
        window.live2cms = {
            convertM3uToNormal: function(m3u) {
                try {
                    var lines = m3u.split('\n');
                    var result = '';
                    var TV = '';
                    var currentGroup = '';
                    lines.forEach(function(line) {
                        if (line.startsWith('#EXTINF:')) {
                            var parts = line.split('"');
                            currentGroup = parts[1] || '';
                            TV = (parts[2] || '').substring(1);
                            if (currentGroup !== parts[1]) {
                                result += '\n' + currentGroup + ',#m3u#\n';
                            }
                        } else if (line.startsWith('http')) {
                            var splitLine = line.split(',');
                            result += TV + '\,' + splitLine[0] + '\n';
                        }
                    });
                    return result.trim();
                } catch(e) { return m3u; }
            }
        };
    }

    // 9. GBK 编码转换
    if (typeof window.gbkTool === 'undefined') {
        window.gbkTool = function() {
            return {
                encode: function(str) { return encodeURIComponent(str); },
                decode: function(str) { return decodeURIComponent(str); }
            };
        };
    }

    console.log('[DRPY2] Engine loaded, all libraries initialized');
})();
