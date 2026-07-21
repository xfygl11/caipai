// ============================================================
// Android WebView Polyfill Shim
// 替换浏览器 API 为原生桥接调用
// 必须在其他 JS 之前加载
// ============================================================

(function() {
    'use strict';

    // ---- localStorage polyfill ----
    var _nativeStorage = (function() {
        if (typeof NativeStorage !== 'undefined') {
            return NativeStorage;
        }
        // Fallback: pure JS localStorage polyfill backed by an in-memory map
        var _memStore = {};
        return {
            getItem: function(key) { return _memStore[key] !== undefined ? _memStore[key] : null; },
            setItem: function(key, val) { _memStore[key] = String(val); },
            removeItem: function(key) { delete _memStore[key]; },
            clear: function() { _memStore = {}; },
            key: function(i) { var k = Object.keys(_memStore); return i >= 0 && i < k.length ? k[i] : null; },
            length: function() { return Object.keys(_memStore).length; }
        };
    })();

    // Replace global localStorage
    if (typeof localStorage === 'undefined') {
        window.localStorage = _nativeStorage;
    } else {
        // Patch existing localStorage to also sync with native bridge
        var _origLsSetItem = localStorage.setItem.bind(localStorage);
        var _origLsGetItem = localStorage.getItem.bind(localStorage);
        localStorage.setItem = function(key, val) {
            _origLsSetItem(key, val);
            try { NativeStorage.setItem(key, val); } catch(e) {}
        };
        localStorage.getItem = function(key) {
            var v = _origLsGetItem(key);
            if (v !== null) return v;
            try { return NativeStorage.getItem(key); } catch(e) { return null; }
        };
    }

    // ---- fetch() polyfill ----
    if (typeof window.fetch === 'undefined') {
        window.fetch = function(url, options) {
            options = options || {};
            var method = (options.method || 'GET').toUpperCase();

            // For non-GET requests, use NativeHttpBridge
            if (method !== 'GET') {
                return new Promise(function(resolve, reject) {
                    try {
                        var body = options.body || null;
                        var cbId = '_fetch_cb_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);
                        window[cbId] = function(response, error) {
                            delete window[cbId];
                            if (error) {
                                reject(new Error(error));
                            } else {
                                resolve(new Response(response, { status: 200 }));
                            }
                        };
                        NativeHttp.httpPost(url, body || '', cbId);
                    } catch(e) {
                        reject(e);
                    }
                });
            }

            // GET request: use synchronous NativeHttp
            return new Promise(function(resolve, reject) {
                try {
                    var text = NativeHttp.httpGet(url);
                    if (text === null) {
                        reject(new Error('Network request failed'));
                        return;
                    }
                    resolve(new Response(text, { status: 200 }));
                } catch(e) {
                    reject(e);
                }
            });
        };
    }

    // ---- Response class polyfill ----
    if (typeof window.Response === 'undefined') {
        window.Response = function(body, init) {
            this.status = (init && init.status) || 200;
            this.ok = this.status >= 200 && this.status < 300;
            this._body = body || '';
        };
        window.Response.prototype.text = function() {
            return Promise.resolve(this._body);
        };
        window.Response.prototype.json = function() {
            var self = this;
            return new Promise(function(resolve, reject) {
                try {
                    resolve(JSON.parse(self._body));
                } catch(e) {
                    reject(e);
                }
            });
        };
        window.Response.prototype.arrayBuffer = function() {
            return Promise.resolve(new ArrayBuffer(0));
        };
        window.Response.prototype.blob = function() {
            return Promise.resolve(new Blob([this._body]));
        };
    }

    // ---- XMLHttpRequest polyfill for DOMParser fallback ----
    // Most code uses fetch, but some legacy code may use XHR
    // We keep DOMParser available for XML parsing in nc-movie-engine.js

    // ---- console.warn fallback ----
    if (typeof console !== 'undefined' && console.warn) {
        var _origWarn = console.warn;
        console.warn = function() {
            try {
                var msg = Array.prototype.join.call(arguments, ' ');
                if (typeof androidBridge !== 'undefined') {
                    androidBridge.log('[WARN] ' + msg);
                }
            } catch(e) {}
            _origWarn.apply(console, arguments);
        };
    }

    // ---- Alert/Confirm polyfill ----
    if (typeof window.alert === 'undefined' || window.alert.toString().indexOf('[native code]') === -1) {
        // Already overridden, skip
    } else {
        var _origAlert = window.alert;
        // Keep native alert for TV remote compatibility
    }

    // ---- ImageBitmap polyfill ----
    if (typeof window.createImageBitmap === 'undefined') {
        window.createImageBitmap = function(blob) {
            return new Promise(function(resolve, reject) {
                var img = new Image();
                img.onload = function() {
                    // Create a canvas to extract bitmap-like data
                    var canvas = document.createElement('canvas');
                    canvas.width = img.naturalWidth;
                    canvas.height = img.naturalHeight;
                    var ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0);
                    // Return a simplified bitmap object
                    resolve({
                        width: img.naturalWidth,
                        height: img.naturalHeight,
                        close: function() {}
                    });
                };
                img.onerror = function() { reject(new Error('ImageBitmap creation failed')); };
                img.src = URL.createObjectURL(blob);
            });
        };
    }

    // ---- BarcodeDetector polyfill ----
    if (typeof window.BarcodeDetector === 'undefined') {
        window.BarcodeDetector = function(opts) {
            this.formats = opts && opts.formats ? opts.formats : ['qr_code'];
        };
        window.BarcodeDetector.prototype.detect = function(image) {
            return Promise.reject(new Error('BarcodeDetector not supported in this WebView'));
        };
    }

    // ---- requestIdleCallback polyfill ----
    if (typeof window.requestIdleCallback === 'undefined') {
        window.requestIdleCallback = function(callback, opts) {
            var deadline = {
                didTimeout: false,
                timeRemaining: function() { return 50; }
            };
            return setTimeout(function() { callback(deadline); }, 1);
        };
    }

    // ---- cancelIdleCallback polyfill ----
    if (typeof window.cancelIdleCallback === 'undefined') {
        window.cancelIdleCallback = function(id) { clearTimeout(id); };
    }

    // ---- URL.createObjectURL polyfill ----
    if (typeof window.URL === 'undefined' || typeof window.URL.createObjectURL === 'undefined') {
        window.URL = window.URL || {};
        window.URL.createObjectURL = function(blob) {
            return 'blob:local-file-' + Date.now();
        };
        window.URL.revokeObjectURL = function() {};
    }

    console.log('[Shim] Android WebView polyfill initialized');
})();
