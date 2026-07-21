// NewCloud 彩票模块：数据、预测、同步、渲染

// UTILS
function R(n){return Math.floor(Math.random()*n)+1}
function S(a){return a.reduce(function(s,v){return s+v},0)}
function LP(v,len){v=String(v);while(v.length<len)v='0'+v;return v}
function P(n){return LP(n,2)}
function SF(a){for(var i=a.length-1;i>0;i--){var j=Math.floor(Math.random()*(i+1));var t=a[i];a[i]=a[j];a[j]=t}return a}
function GPN(p,k){var s=String(parseInt(p)+1);return k==='ssq'?LP(s,7):LP(s,5)}
function periodNum(p){return parseInt(String(p||'').replace(/\D/g,''),10)||0}
function isOnlineMode(){return location.protocol==='http:'||location.protocol==='https:'}
function canUseLocalApi(){return location.protocol==='http:'||location.protocol==='https:'}
function isAppMode(){return !!window.AndroidSync}
function hasDup(a){var o={};for(var i=0;i<a.length;i++){if(o[a[i]])return true;o[a[i]]=1}return false}

// EMBEDDED DATA
var DLT_HISTORY=[{p:'2026071',d:'2026-06-27',f:[5,13,22,26,32],b:[2,3],sales:'3.33亿',pool:'8.44亿',grades:[{type:'一等奖',num:'1',money:'10,000,000元'},{type:'一等奖追加',num:'0',money:'0元'}]},{p:'2026070',d:'2026-06-24',f:[4,5,15,21,32],b:[2,11]},{p:'2026069',d:'2026-06-22',f:[12,19,21,24,29],b:[3,10]},{p:'2026068',d:'2026-06-20',f:[3,11,12,21,22],b:[6,10]},{p:'2026067',d:'2026-06-17',f:[6,16,18,19,28],b:[7,11]},{p:'2026066',d:'2026-06-15',f:[10,13,19,21,30],b:[4,5]},{p:'2026065',d:'2026-06-13',f:[4,11,12,13,25],b:[4,8]},{p:'2026064',d:'2026-06-10',f:[3,13,15,17,21],b:[2,7]},{p:'2026063',d:'2026-06-08',f:[3,15,20,29,31],b:[1,12]},{p:'2026062',d:'2026-06-06',f:[7,15,20,24,29],b:[4,10]},{p:'2026061',d:'2026-06-03',f:[10,12,26,31,35],b:[2,12]},{p:'2026060',d:'2026-06-01',f:[22,28,30,31,34],b:[1,5]},{p:'2026059',d:'2026-05-30',f:[6,13,17,19,26],b:[7,8]},{p:'2026058',d:'2026-05-27',f:[7,12,13,18,34],b:[1,5]},{p:'2026057',d:'2026-05-25',f:[23,25,26,27,34],b:[4,10]},{p:'2026056',d:'2026-05-23',f:[6,7,18,21,30],b:[1,5]},{p:'2026055',d:'2026-05-20',f:[9,10,20,33,35],b:[4,11]},{p:'2026054',d:'2026-05-18',f:[2,6,14,22,24],b:[8,11]},{p:'2026053',d:'2026-05-16',f:[2,9,14,20,31],b:[5,9]},{p:'2026052',d:'2026-05-13',f:[2,3,20,28,33],b:[2,12]},{p:'2026051',d:'2026-05-11',f:[13,18,28,32,33],b:[2,11]},{p:'2026050',d:'2026-05-09',f:[6,10,14,23,33],b:[8,10]},{p:'2026049',d:'2026-05-06',f:[1,6,14,15,17],b:[2,3]},{p:'2026048',d:'2026-05-04',f:[11,17,20,23,35],b:[1,10]},{p:'2026047',d:'2026-05-02',f:[9,20,21,23,28],b:[6,11]},{p:'2026046',d:'2026-04-29',f:[1,13,18,27,33],b:[4,7]},{p:'2026045',d:'2026-04-27',f:[1,15,21,26,33],b:[4,7]},{p:'2026044',d:'2026-04-25',f:[3,8,22,26,29],b:[7,10]},{p:'2026043',d:'2026-04-22',f:[8,12,14,19,22],b:[11,12]},{p:'2026042',d:'2026-04-20',f:[2,7,13,19,24],b:[3,8]},{p:'2026041',d:'2026-04-18',f:[24,25,27,29,34],b:[2,6]},{p:'2026040',d:'2026-04-15',f:[6,12,13,21,34],b:[8,9]},{p:'2026039',d:'2026-04-13',f:[9,11,20,26,27],b:[6,9]},{p:'2026038',d:'2026-04-11',f:[8,17,21,33,35],b:[6,7]},{p:'2026037',d:'2026-04-08',f:[7,12,13,28,32],b:[6,8]},{p:'2026036',d:'2026-04-06',f:[4,7,16,26,32],b:[5,8]},{p:'2026035',d:'2026-04-04',f:[2,22,30,33,34],b:[8,12]},{p:'2026034',d:'2026-04-01',f:[11,12,25,26,27],b:[8,11]},{p:'2026033',d:'2026-03-30',f:[3,5,7,9,18],b:[2,10]},{p:'2026032',d:'2026-03-28',f:[3,4,19,26,32],b:[1,12]},{p:'2026031',d:'2026-03-25',f:[6,8,22,29,34],b:[5,7]},{p:'2026030',d:'2026-03-23',f:[2,13,22,28,34],b:[5,12]},{p:'2026029',d:'2026-03-21',f:[3,5,17,33,35],b:[5,7]},{p:'2026028',d:'2026-03-18',f:[15,27,29,30,34],b:[1,10]},{p:'2026027',d:'2026-03-14',f:[6,8,17,18,32],b:[3,12]},{p:'2026026',d:'2026-03-11',f:[9,13,15,29,35],b:[2,6]},{p:'2026025',d:'2026-03-09',f:[3,15,24,28,29],b:[3,7]},{p:'2026024',d:'2026-03-07',f:[2,4,8,10,21],b:[9,12]},{p:'2026023',d:'2026-03-04',f:[9,25,26,27,28],b:[1,8]},{p:'2026022',d:'2026-03-02',f:[5,9,10,18,26],b:[5,6]},{p:'2026021',d:'2026-02-28',f:[5,8,12,14,17],b:[4,5]},{p:'2026020',d:'2026-02-25',f:[1,10,21,23,29],b:[10,12]},{p:'2026019',d:'2026-02-23',f:[12,13,14,16,31],b:[4,12]},{p:'2026018',d:'2026-02-11',f:[9,11,19,30,35],b:[1,12]},{p:'2026017',d:'2026-02-09',f:[4,5,10,23,31],b:[7,12]},{p:'2026016',d:'2026-02-07',f:[8,9,12,19,24],b:[1,6]},{p:'2026015',d:'2026-02-04',f:[1,4,10,13,17],b:[3,11]},{p:'2026014',d:'2026-02-02',f:[16,18,23,34,35],b:[1,6]},{p:'2026013',d:'2026-01-31',f:[3,5,6,23,26],b:[1,4]},{p:'2026012',d:'2026-01-28',f:[1,2,9,22,25],b:[1,6]},{p:'2026011',d:'2026-01-26',f:[14,21,23,29,33],b:[2,10]},{p:'2026010',d:'2026-01-24',f:[2,3,13,18,26],b:[2,9]},{p:'2026009',d:'2026-01-21',f:[5,12,13,14,33],b:[5,8]},{p:'2026008',d:'2026-01-19',f:[3,6,17,21,33],b:[5,11]},{p:'2026007',d:'2026-01-17',f:[1,3,13,20,26],b:[3,10]},{p:'2026006',d:'2026-01-14',f:[5,12,18,23,35],b:[6,12]},{p:'2026005',d:'2026-01-12',f:[2,4,16,23,35],b:[6,11]},{p:'2026004',d:'2026-01-10',f:[5,18,23,25,32],b:[5,9]},{p:'2026003',d:'2026-01-07',f:[2,9,11,15,16],b:[2,4]},{p:'2026002',d:'2026-01-05',f:[4,8,15,20,31],b:[7,8]},{p:'2026001',d:'2026-01-03',f:[7,9,23,27,32],b:[2,8]},{p:'2025150',d:'2025-12-31',f:[13,14,15,28,31],b:[1,5]},{p:'2025149',d:'2025-12-29',f:[24,26,30,31,32],b:[5,12]},{p:'2025148',d:'2025-12-27',f:[3,4,14,30,32],b:[8,12]},{p:'2025147',d:'2025-12-24',f:[6,16,21,25,33],b:[7,8]},{p:'2025146',d:'2025-12-22',f:[6,11,13,16,22],b:[2,3]},{p:'2025145',d:'2025-12-20',f:[5,7,20,22,25],b:[4,5]},{p:'2025144',d:'2025-12-17',f:[2,5,13,15,28],b:[5,8]},{p:'2025143',d:'2025-12-15',f:[3,4,18,24,29],b:[7,12]},{p:'2025142',d:'2025-12-13',f:[9,10,14,27,29],b:[2,9]},{p:'2025141',d:'2025-12-10',f:[4,9,24,28,29],b:[2,10]},{p:'2025140',d:'2025-12-08',f:[4,5,13,18,34],b:[2,8]},{p:'2025139',d:'2025-12-06',f:[8,18,22,30,35],b:[1,4]},{p:'2025138',d:'2025-12-03',f:[1,3,19,21,23],b:[7,11]},{p:'2025137',d:'2025-12-01',f:[7,8,9,11,22],b:[5,11]},{p:'2025136',d:'2025-11-29',f:[7,11,15,16,23],b:[9,11]},{p:'2025135',d:'2025-11-26',f:[2,10,16,28,32],b:[1,7]},{p:'2025134',d:'2025-11-24',f:[7,12,18,27,33],b:[9,10]},{p:'2025133',d:'2025-11-22',f:[4,11,23,27,35],b:[7,11]},{p:'2025132',d:'2025-11-19',f:[1,9,10,12,19],b:[6,7]},{p:'2025131',d:'2025-11-17',f:[3,8,25,29,32],b:[9,12]},{p:'2025130',d:'2025-11-15',f:[1,13,16,27,29],b:[2,11]},{p:'2025129',d:'2025-11-12',f:[3,9,14,28,35],b:[2,4]},{p:'2025128',d:'2025-11-10',f:[3,6,26,30,33],b:[11,12]}];

var SSQ_HISTORY=[{p:'2026073',d:'2026-06-28',f:[9,10,13,16,19,21],b:[8],sales:'398,594,042元',pool:'328,183,968元',grades:[{type:'一等奖',num:'10',money:'6,111,492元'},{type:'二等奖',num:'136',money:'326,909元'},{type:'三等奖',num:'1,395',money:'3,000元'},{type:'四等奖',num:'74,961',money:'200元'},{type:'五等奖',num:'1,464,286',money:'10元'},{type:'六等奖',num:'11,239,744',money:'5元'},{type:'福彩奖',num:'9,943,535',money:'5元'}]},{p:'2026072',d:'2026-06-25',f:[7,8,12,15,17,21],b:[1]},{p:'2026071',d:'2026-06-23',f:[3,8,19,25,31,33],b:[5]},{p:'2026070',d:'2026-06-21',f:[3,6,8,14,26,27],b:[8]},{p:'2026069',d:'2026-06-18',f:[12,14,16,17,18,32],b:[8]},{p:'2026068',d:'2026-06-16',f:[3,5,16,18,29,32],b:[4]},{p:'2026067',d:'2026-06-14',f:[4,19,27,29,30,32],b:[13]},{p:'2026066',d:'2026-06-11',f:[5,11,21,23,24,29],b:[16]},{p:'2026065',d:'2026-06-09',f:[7,8,16,24,30,32],b:[2]},{p:'2026064',d:'2026-06-07',f:[1,9,15,18,29,33],b:[15]},{p:'2026063',d:'2026-06-04',f:[2,8,25,28,30,31],b:[2]},{p:'2026062',d:'2026-06-02',f:[2,4,7,14,28,29],b:[9]},{p:'2026061',d:'2026-05-31',f:[1,4,5,15,23,28],b:[7]},{p:'2026060',d:'2026-05-28',f:[7,9,10,16,22,27],b:[11]},{p:'2026059',d:'2026-05-26',f:[8,16,26,28,29,30],b:[15]},{p:'2026058',d:'2026-05-24',f:[1,4,7,21,29,30],b:[1]},{p:'2026057',d:'2026-05-21',f:[1,10,22,24,28,30],b:[7]},{p:'2026056',d:'2026-05-19',f:[10,19,21,22,31,33],b:[5]},{p:'2026055',d:'2026-05-17',f:[4,11,24,25,32,33],b:[13]},{p:'2026054',d:'2026-05-14',f:[13,20,25,29,30,33],b:[2]},{p:'2026053',d:'2026-05-12',f:[1,2,3,8,13,14],b:[2]},{p:'2026052',d:'2026-05-10',f:[1,3,11,22,26,31],b:[11]},{p:'2026051',d:'2026-05-07',f:[9,14,15,16,29,30],b:[10]},{p:'2026050',d:'2026-05-05',f:[6,9,25,27,28,30],b:[3]},{p:'2026049',d:'2026-05-03',f:[3,4,14,15,18,20],b:[2]},{p:'2026048',d:'2026-04-30',f:[9,15,18,24,28,33],b:[1]},{p:'2026047',d:'2026-04-28',f:[7,16,21,24,27,30],b:[7]},{p:'2026046',d:'2026-04-26',f:[2,9,10,24,31,33],b:[16]},{p:'2026045',d:'2026-04-23',f:[4,11,15,17,24,30],b:[15]},{p:'2026044',d:'2026-04-21',f:[2,14,17,18,22,30],b:[1]},{p:'2026043',d:'2026-04-19',f:[6,9,14,16,25,32],b:[16]},{p:'2026042',d:'2026-04-16',f:[2,7,12,19,24,31],b:[10]},{p:'2026041',d:'2026-04-14',f:[2,8,10,17,19,24],b:[13]},{p:'2026040',d:'2026-04-12',f:[3,4,14,22,23,33],b:[4]},{p:'2026039',d:'2026-04-09',f:[8,17,18,21,25,30],b:[5]},{p:'2026038',d:'2026-04-07',f:[1,2,13,23,25,27],b:[5]},{p:'2026037',d:'2026-04-05',f:[11,22,27,29,31,33],b:[12]},{p:'2026036',d:'2026-04-02',f:[6,10,12,15,22,28],b:[8]},{p:'2026035',d:'2026-03-31',f:[2,6,12,24,25,32],b:[2]},{p:'2026034',d:'2026-03-29',f:[1,3,7,13,22,23],b:[7]},{p:'2026033',d:'2026-03-26',f:[3,6,13,21,28,29],b:[6]},{p:'2026032',d:'2026-03-24',f:[1,3,11,18,31,33],b:[2]},{p:'2026031',d:'2026-03-22',f:[3,10,12,13,18,33],b:[8]},{p:'2026030',d:'2026-03-19',f:[10,11,14,19,22,24],b:[4]},{p:'2026029',d:'2026-03-17',f:[6,19,22,23,28,31],b:[5]},{p:'2026028',d:'2026-03-15',f:[2,6,9,17,25,28],b:[15]},{p:'2026027',d:'2026-03-12',f:[2,13,17,18,25,26],b:[13]},{p:'2026026',d:'2026-03-10',f:[2,9,16,22,25,29],b:[3]},{p:'2026025',d:'2026-03-08',f:[2,3,15,20,23,24],b:[10]},{p:'2026024',d:'2026-03-05',f:[1,2,13,21,23,29],b:[14]},{p:'2026023',d:'2026-03-03',f:[1,3,8,10,23,29],b:[6]},{p:'2026022',d:'2026-03-01',f:[15,18,23,25,28,32],b:[11]},{p:'2026021',d:'2026-02-26',f:[3,13,25,26,30,31],b:[4]},{p:'2026020',d:'2026-02-24',f:[1,13,14,21,24,30],b:[2]},{p:'2026019',d:'2026-02-12',f:[7,8,16,17,18,30],b:[1]},{p:'2026018',d:'2026-02-10',f:[11,15,17,22,25,30],b:[7]},{p:'2026017',d:'2026-02-08',f:[1,3,5,18,29,32],b:[4]},{p:'2026016',d:'2026-02-05',f:[4,5,9,10,27,30],b:[13]},{p:'2026015',d:'2026-02-03',f:[7,10,13,22,27,31],b:[12]},{p:'2026014',d:'2026-02-01',f:[7,13,19,22,26,32],b:[1]},{p:'2026013',d:'2026-01-29',f:[4,9,12,13,16,20],b:[1]},{p:'2026012',d:'2026-01-27',f:[3,5,7,16,20,24],b:[8]},{p:'2026011',d:'2026-01-25',f:[2,3,4,20,31,32],b:[4]},{p:'2026010',d:'2026-01-22',f:[4,9,10,15,19,26],b:[12]},{p:'2026009',d:'2026-01-20',f:[3,6,13,19,23,25],b:[10]},{p:'2026008',d:'2026-01-18',f:[6,9,16,27,31,33],b:[10]},{p:'2026007',d:'2026-01-15',f:[9,13,19,27,29,30],b:[1]},{p:'2026006',d:'2026-01-13',f:[2,6,22,23,24,28],b:[15]},{p:'2026005',d:'2026-01-11',f:[1,20,22,27,30,33],b:[10]},{p:'2026004',d:'2026-01-08',f:[3,7,8,9,18,32],b:[10]},{p:'2026003',d:'2026-01-06',f:[5,6,9,21,28,30],b:[16]},{p:'2026002',d:'2026-01-04',f:[1,5,7,18,30,32],b:[2]},{p:'2026001',d:'2026-01-01',f:[2,6,11,12,13,33],b:[15]},{p:'2025151',d:'2025-12-30',f:[8,9,14,22,28,30],b:[4]},{p:'2025150',d:'2025-12-28',f:[6,13,17,19,24,31],b:[8]},{p:'2025149',d:'2025-12-25',f:[1,2,4,6,22,30],b:[10]},{p:'2025148',d:'2025-12-23',f:[3,4,9,10,15,22],b:[16]},{p:'2025147',d:'2025-12-21',f:[1,3,5,8,22,33],b:[8]},{p:'2025146',d:'2025-12-18',f:[5,7,12,24,26,28],b:[2]},{p:'2025145',d:'2025-12-16',f:[11,12,15,18,25,32],b:[14]},{p:'2025144',d:'2025-12-14',f:[1,8,15,20,26,33],b:[13]},{p:'2025143',d:'2025-12-11',f:[2,9,12,13,15,24],b:[3]},{p:'2025142',d:'2025-12-09',f:[2,13,15,23,27,31],b:[16]},{p:'2025141',d:'2025-12-07',f:[2,4,5,10,12,13],b:[6]},{p:'2025140',d:'2025-12-04',f:[1,3,4,12,18,24],b:[5]},{p:'2025139',d:'2025-12-02',f:[2,5,17,22,30,33],b:[6]},{p:'2025138',d:'2025-11-30',f:[10,13,14,23,24,27],b:[15]},{p:'2025137',d:'2025-11-27',f:[2,8,11,23,27,29],b:[5]},{p:'2025136',d:'2025-11-25',f:[8,10,14,23,28,32],b:[12]},{p:'2025135',d:'2025-11-23',f:[1,2,5,9,25,32],b:[10]},{p:'2025134',d:'2025-11-20',f:[3,5,9,13,26,29],b:[12]},{p:'2025133',d:'2025-11-18',f:[5,14,17,19,20,33],b:[7]},{p:'2025132',d:'2025-11-16',f:[4,8,10,21,23,32],b:[11]},{p:'2025131',d:'2025-11-13',f:[3,13,14,18,24,31],b:[3]},{p:'2025130',d:'2025-11-11',f:[1,5,8,14,19,23],b:[6]},{p:'2025129',d:'2025-11-09',f:[3,4,7,13,20,30],b:[3]},{p:'2025128',d:'2025-11-06',f:[2,10,18,19,24,27],b:[1]},{p:'2025127',d:'2025-11-04',f:[3,9,15,17,19,28],b:[3]},{p:'2025126',d:'2025-11-02',f:[2,12,13,16,19,25],b:[10]}];

// CONFIG
var CFG={dlt:{name:'大乐透',fMax:35,fCnt:5,bMax:12,bCnt:2,dd:[1,3,6],dh:20,dm:40,api:'/api/dlt/latest',zones:[12,12,11],fLabel:'前区',bLabel:'后区'},ssq:{name:'双色球',fMax:33,fCnt:6,bMax:16,bCnt:1,dd:[2,4,0],dh:21,dm:15,api:'/api/ssq/latest',zones:[11,11,11],fLabel:'红球',bLabel:'蓝球'}};
var LOTTERIES=[
{id:'dlt',name:'超级大乐透',type:'体彩',desc:'前区35选5，后区12选2',draw:'每周一、三、六 21:25开奖',rule:'从01-35中选5个前区号码，从01-12中选2个后区号码。每注7个号码，开奖时按前区和后区匹配情况确定奖级。',mode:'balls',sets:[{max:35,cnt:5,cls:'f'},{max:12,cnt:2,cls:'b'}]},
{id:'ssq',name:'双色球',type:'福彩',desc:'红球33选6，蓝球16选1',draw:'每周二、四、日 21:15开奖',rule:'从01-33中选6个红球号码，从01-16中选1个蓝球号码。每注7个号码，按红球和蓝球命中数量确定奖级。',mode:'balls',sets:[{max:33,cnt:6,cls:'f'},{max:16,cnt:1,cls:'b'}]},
{id:'qlc',name:'七乐彩',type:'福彩',desc:'30选7，另开特别号',draw:'每周一、三、五 20:40开奖',rule:'从01-30中选择7个基本号码，开奖开出7个基本号码和1个特别号码。按基本号码和特别号码命中情况确定奖级。',mode:'balls',sets:[{max:30,cnt:7,cls:'f'},{max:30,cnt:1,cls:'b'}]},
{id:'fc3d',name:'福彩3D',type:'福彩',desc:'福彩三位数字，000-999',draw:'每天 20:40开奖',rule:'福彩3D属于中国福利彩票，选择000-999范围内的三位数字，可按直选、组选等方式投注。直选要求百位、十位、个位完全一致。',mode:'digits',len:3},
{id:'pl3',name:'排列3',type:'体彩',desc:'中国体育彩票三位数字',draw:'每天 21:25开奖',rule:'排列3属于中国体育彩票，选择000-999范围内的三位数字，按百位、十位、个位开奖。第26170期开奖号码为4 1 7。',mode:'digits',len:3},
{id:'pl5',name:'排列5',type:'体彩',desc:'中国体育彩票五位数字',draw:'每天 21:25开奖',rule:'排列5属于中国体育彩票，选择00000-99999范围内的五位数字，要求五位号码与开奖号码按位全部一致。第26170期开奖号码为4 1 7 7 2。',mode:'digits',len:5},
{id:'qxc',name:'7星彩',type:'体彩',desc:'中国体育彩票七位数字',draw:'每周二、五、日 21:25开奖',rule:'7星彩属于中国体育彩票，选择七位号码参与开奖，按各位号码匹配情况确定奖级。第26073期开奖号码为6 6 8 4 8 9 0。',mode:'digits',len:7},
{id:'kl8',name:'快乐8',type:'福彩',desc:'80选20官方开奖，选十预测',draw:'每天 20:40开奖',rule:'从01-80中选择1至10个号码参与不同玩法。这里默认生成选十玩法的10个号码；同步开奖结果时显示官方20个开奖号码。',mode:'balls',sets:[{max:80,cnt:10,cls:'f'}]}
];
var LOT_DEFAULT_HISTORY={
qlc:[
{p:'2026073',d:'2026-06-29',sales:'2,803,400元',pool:'654,014元',grades:[{type:'一等奖',num:'0',money:'0元'},{type:'二等奖',num:'4',money:'23,357元'},{type:'三等奖',num:'76',money:'2,458元'},{type:'四等奖',num:'375',money:'200元'},{type:'五等奖',num:'2,640',money:'50元'},{type:'六等奖',num:'6,652',money:'10元'},{type:'七等奖',num:'33,168',money:'5元'}],pick:[{cls:'f',nums:[1,3,6,16,25,27,29]},{cls:'b',nums:[9]}]},
{p:'2026072',d:'2026-06-26',pick:[{cls:'f',nums:[14,16,17,24,25,27,28]},{cls:'b',nums:[9]}]},
{p:'2026071',d:'2026-06-24',pick:[{cls:'f',nums:[10,12,14,20,25,26,30]},{cls:'b',nums:[21]}]},
{p:'2026070',d:'2026-06-22',pick:[{cls:'f',nums:[2,4,7,8,14,24,27]},{cls:'b',nums:[16]}]},
{p:'2026069',d:'2026-06-19',pick:[{cls:'f',nums:[3,4,12,14,15,27,30]},{cls:'b',nums:[28]}]},
{p:'2026068',d:'2026-06-17',pick:[{cls:'f',nums:[6,7,12,13,22,25,26]},{cls:'b',nums:[27]}]},
{p:'2026067',d:'2026-06-15',pick:[{cls:'f',nums:[2,5,15,17,23,25,30]},{cls:'b',nums:[16]}]},
{p:'2026066',d:'2026-06-12',pick:[{cls:'f',nums:[10,11,12,13,14,15,17]},{cls:'b',nums:[6]}]},
{p:'2026065',d:'2026-06-10',pick:[{cls:'f',nums:[4,9,17,18,24,27,29]},{cls:'b',nums:[5]}]},
{p:'2026064',d:'2026-06-08',pick:[{cls:'f',nums:[4,6,10,13,16,18,26]},{cls:'b',nums:[27]}]}],
fc3d:[
{p:'2026170',d:'2026-06-29',sales:'117,166,228元',pool:'',pick:[{cls:'d',nums:[8,0,5]}]},
{p:'2026169',d:'2026-06-28',pick:[{cls:'d',nums:[8,3,2]}]},
{p:'2026168',d:'2026-06-27',pick:[{cls:'d',nums:[1,3,0]}]},
{p:'2026167',d:'2026-06-26',pick:[{cls:'d',nums:[6,3,1]}]},
{p:'2026166',d:'2026-06-25',pick:[{cls:'d',nums:[9,0,0]}]},
{p:'2026165',d:'2026-06-24',pick:[{cls:'d',nums:[4,2,4]}]},
{p:'2026164',d:'2026-06-23',pick:[{cls:'d',nums:[6,9,0]}]},
{p:'2026163',d:'2026-06-22',pick:[{cls:'d',nums:[5,3,7]}]},
{p:'2026162',d:'2026-06-21',pick:[{cls:'d',nums:[5,8,5]}]},
{p:'2026161',d:'2026-06-20',pick:[{cls:'d',nums:[5,2,9]}]}],
pl3:[
{p:'2026170',d:'2026-06-29',sales:'40,846,786元',pool:'',grades:[{type:'直选',num:'17,210',money:'1,040元'},{type:'组选3',num:'0',money:'346元'},{type:'组选6',num:'57,879',money:'173元'}],pick:[{cls:'d',nums:[4,1,7]}]},
{p:'26169',d:'2026-06-28',pick:[{cls:'d',nums:[8,8,4]}]},
{p:'26168',d:'2026-06-27',pick:[{cls:'d',nums:[1,9,2]}]},
{p:'26167',d:'2026-06-26',pick:[{cls:'d',nums:[0,1,2]}]},
{p:'26166',d:'2026-06-25',pick:[{cls:'d',nums:[2,2,9]}]},
{p:'26165',d:'2026-06-24',pick:[{cls:'d',nums:[2,9,4]}]},
{p:'26164',d:'2026-06-23',pick:[{cls:'d',nums:[7,1,5]}]},
{p:'26163',d:'2026-06-22',pick:[{cls:'d',nums:[3,4,6]}]},
{p:'26162',d:'2026-06-21',pick:[{cls:'d',nums:[3,6,9]}]},
{p:'26161',d:'2026-06-20',pick:[{cls:'d',nums:[5,6,2]}]}],
pl5:[
{p:'26170',d:'2026-06-29',pick:[{cls:'d',nums:[4,1,7,7,2]}]},
{p:'26169',d:'2026-06-28',pick:[{cls:'d',nums:[8,8,4,7,3]}]},
{p:'26168',d:'2026-06-27',pick:[{cls:'d',nums:[1,9,2,7,4]}]},
{p:'26167',d:'2026-06-26',pick:[{cls:'d',nums:[0,1,2,3,1]}]},
{p:'26166',d:'2026-06-25',pick:[{cls:'d',nums:[2,2,9,5,0]}]},
{p:'26165',d:'2026-06-24',pick:[{cls:'d',nums:[2,9,4,4,9]}]},
{p:'26164',d:'2026-06-23',pick:[{cls:'d',nums:[7,1,5,7,2]}]},
{p:'26163',d:'2026-06-22',pick:[{cls:'d',nums:[3,4,6,4,9]}]},
{p:'26162',d:'2026-06-21',pick:[{cls:'d',nums:[3,6,9,6,3]}]},
{p:'26161',d:'2026-06-20',pick:[{cls:'d',nums:[5,6,2,0,5]}]}],
qxc:[
{p:'26073',d:'2026-06-28',sales:'17,325,444元',pool:'289,203,141元',grades:[{type:'一等奖',num:'0',money:'0元'},{type:'二等奖',num:'3',money:'139,757元'},{type:'三等奖',num:'30',money:'3,000元'},{type:'四等奖',num:'1,053',money:'500元'},{type:'五等奖',num:'18,111',money:'30元'},{type:'六等奖',num:'627,381',money:'5元'}],pick:[{cls:'d',nums:[6,6,8,4,8,9,0]}]},
{p:'26072',d:'2026-06-26',pick:[{cls:'d',nums:[0,8,3,4,5,6,6]}]},
{p:'26071',d:'2026-06-23',pick:[{cls:'d',nums:[4,7,9,6,3,5,7]}]},
{p:'26070',d:'2026-06-21',pick:[{cls:'d',nums:[3,1,5,1,4,6,6]}]},
{p:'26069',d:'2026-06-19',pick:[{cls:'d',nums:[7,9,5,4,3,2,7]}]},
{p:'26068',d:'2026-06-16',pick:[{cls:'d',nums:[7,5,1,1,6,2,5]}]},
{p:'26067',d:'2026-06-14',pick:[{cls:'d',nums:[3,6,5,7,1,4,3]}]},
{p:'26066',d:'2026-06-12',pick:[{cls:'d',nums:[3,5,6,9,7,0,12]}]},
{p:'26065',d:'2026-06-09',pick:[{cls:'d',nums:[7,4,9,0,7,3,1]}]},
{p:'26064',d:'2026-06-07',pick:[{cls:'d',nums:[2,3,3,7,2,5,9]}]}],
kl8:[
{p:'2026170',d:'2026-06-29',sales:'99,498,812.00元',pool:'151,317,005.80元',grades:[{type:'选十中十',num:'0',money:'0元'},{type:'选十中九',num:'62',money:'8,000元'},{type:'选十中八',num:'1,032',money:'720元'},{type:'选十中七',num:'12,821',money:'80元'},{type:'选十中六',num:'90,242',money:'5元'},{type:'选十中五',num:'388,994',money:'3元'},{type:'选十中0',num:'263,883',money:'2元'},{type:'选九中九',num:'0',money:'0元'},{type:'选九中八',num:'180',money:'2,000元'},{type:'选九中七',num:'2,924',money:'225元'},{type:'选九中六',num:'27,325',money:'22元'},{type:'选九中五',num:'149,549',money:'5元'}],pick:[{cls:'f',nums:[2,3,7,9,16,20,21,23,24,25,27,33,35,36,41,43,49,51,57,65]}]},
{p:'2026169',d:'2026-06-28',pick:[{cls:'f',nums:[4,5,6,18,21,25,26,27,31,33,37,39,40,43,52,56,58,61,62,73]}]},
{p:'2026168',d:'2026-06-27',pick:[{cls:'f',nums:[2,6,8,9,20,21,24,25,31,33,42,44,46,48,55,61,64,66,72,77]}]},
{p:'2026167',d:'2026-06-26',pick:[{cls:'f',nums:[1,3,12,13,15,16,17,22,24,28,33,34,35,40,45,47,54,66,68,74]}]},
{p:'2026166',d:'2026-06-25',pick:[{cls:'f',nums:[2,8,14,16,17,24,25,27,30,31,41,48,50,55,61,70,71,73,76,79]}]},
{p:'2026165',d:'2026-06-24',pick:[{cls:'f',nums:[1,9,10,12,26,31,33,35,39,42,46,48,49,57,61,64,65,66,67,74]}]},
{p:'2026164',d:'2026-06-23',pick:[{cls:'f',nums:[1,3,4,5,13,17,19,25,30,34,38,46,56,58,59,60,63,69,76,80]}]},
{p:'2026163',d:'2026-06-22',pick:[{cls:'f',nums:[8,9,11,14,18,22,28,35,37,40,41,42,45,50,54,58,70,71,73,74]}]},
{p:'2026162',d:'2026-06-21',pick:[{cls:'f',nums:[1,2,8,13,16,18,19,30,33,37,39,40,56,57,63,69,71,73,76,78]}]},
{p:'2026161',d:'2026-06-20',pick:[{cls:'f',nums:[2,14,15,23,28,30,35,39,43,44,49,50,51,53,57,60,62,69,74,76]}]}]
};

// PREDICTION
window._preds={};
window._preds.dlt={pending:loadP('dlt'),history:loadH('dlt')};
window._preds.ssq={pending:loadP('ssq'),history:loadH('ssq')};

var ALGO_NAMES=['热号频率','冷号补位','区间均衡','和值跨度','混合模型'];
function freqData(k){
  var c=CFG[k],hist=k==='dlt'?DLT_HISTORY:SSQ_HISTORY;
  var fF=new Array(c.fMax+1).fill(0),fB=new Array(c.bMax+1).fill(0);
  var n=Math.min(hist.length,50);
  for(var i=0;i<n;i++){var r=hist[i];r.f.forEach(function(x){fF[x]++});r.b.forEach(function(x){fB[x]++})}
  return {fF:fF,fB:fB,n:n};
}
function pickByWeight(nums,weights,cnt){
  var arr=nums.slice().sort(function(a,b){return weights[b-1]-weights[a-1]});
  var pool=arr.slice(0,Math.max(cnt*3,Math.min(arr.length,18)));
  SF(pool);
  return pool.slice(0,cnt).sort(function(a,b){return a-b});
}
function predict(k,algo){
  return new Promise(function(resolve) {
    setTimeout(function() {
      var c=CFG[k],fd=freqData(k),nf=[],nb=[];
      for(var i=1;i<=c.fMax;i++)nf.push(i);
      for(var j=1;j<=c.bMax;j++)nb.push(j);
      var bestF=null,bestB=null,bestS=-1e9,rounds=c.fCnt===6?650:780;
      var chunkSize=50,started=0;
      function runChunk() {
        var end=Math.min(started+chunkSize,rounds);
        for(;started<end;started++){
          var wf=nf.map(function(x){
            if(algo===0)return fd.fF[x]*1.2+Math.random()*3;
            if(algo===1)return (fd.n-fd.fF[x])*0.7+Math.random()*4;
            if(algo===2)return fd.fF[x]*0.55+Math.random()*5;
            if(algo===3)return fd.fF[x]*0.35+Math.random()*6;
            return fd.fF[x]*0.8+(fd.n-fd.fF[x])*0.25+Math.random()*5;
          });
          var wb=nb.map(function(x){
            if(algo===0)return fd.fB[x]*1.1+Math.random()*3;
            if(algo===1)return (fd.n-fd.fB[x])*0.7+Math.random()*4;
            if(algo===2)return fd.fB[x]*0.5+Math.random()*5;
            if(algo===3)return fd.fB[x]*0.3+Math.random()*6;
            return fd.fB[x]*0.75+(fd.n-fd.fB[x])*0.25+Math.random()*5;
          });
          var f=pickByWeight(nf,wf,c.fCnt),b=pickByWeight(nb,wb,c.bCnt);
          var odds=f.filter(function(x){return x%2===1}).length;
          var fs=S(f),ideal=(c.fMax+1)*c.fCnt/2,span=f[f.length-1]-f[0];
          var z=[0,0,0];f.forEach(function(x){if(x<=c.zones[0])z[0]++;else if(x<=c.zones[0]+c.zones[1])z[1]++;else z[2]++});
          var sc=0;
          f.forEach(function(x){sc+=fd.fF[x]*(algo===1?-1:2)});b.forEach(function(x){sc+=fd.fB[x]*(algo===1?-1:2)});
          sc-=Math.abs(odds-c.fCnt/2)*(algo===2?20:10);
          sc-=Math.abs(fs-ideal)*(algo===3?0.75:0.25);
          sc-=Math.abs(span-(c.fMax*0.62))*(algo===3?1.8:0.25);
          if(algo===2)sc-=Math.abs(z[0]-z[1])*8+Math.abs(z[1]-z[2])*8;
          if(sc>bestS){bestS=sc;bestF=f.slice();bestB=b.slice()}
        }
        if(started<rounds){
          setTimeout(runChunk, 0);
        } else {
          resolve({f:bestF,b:bestB,algo:algo,name:ALGO_NAMES[algo]});
        }
      }
      runChunk();
    }, 0);
  });
}
function makeGroups(k){
  return new Promise(function(resolve) {
    var gs=[],seen={},algoIdx=0;
    function predictNext() {
      if(algoIdx>=5){resolve(gs);return}
      var idx=algoIdx;
      predict(k,idx).then(function(p){
        var key=p.f.join(',')+'+'+p.b.join(',');
        var guard=0;
        while(seen[key]&&guard<8){
          algoIdx++;
          if(algoIdx>=5){resolve(gs);return}
          p=window._predictSync(k,algoIdx);
          key=p.f.join(',')+'+'+p.b.join(',');
          guard++;
        }
        if(!seen[key]){seen[key]=1;gs.push(p)}
        algoIdx++;
        setTimeout(predictNext, 0);
      });
    }
    predictNext();
  });
}
// Sync fallback for normalizeEntry
function _predictSync(k,algo){
  var c=CFG[k],fd=freqData(k),nf=[],nb=[];
  for(var i=1;i<=c.fMax;i++)nf.push(i);
  for(var j=1;j<=c.bMax;j++)nb.push(j);
  var bestF=null,bestB=null,bestS=-1e9,rounds=c.fCnt===6?650:780;
  for(var t=0;t<rounds;t++){
    var wf=nf.map(function(x){
      if(algo===0)return fd.fF[x]*1.2+Math.random()*3;
      if(algo===1)return (fd.n-fd.fF[x])*0.7+Math.random()*4;
      if(algo===2)return fd.fF[x]*0.55+Math.random()*5;
      if(algo===3)return fd.fF[x]*0.35+Math.random()*6;
      return fd.fF[x]*0.8+(fd.n-fd.fF[x])*0.25+Math.random()*5;
    });
    var wb=nb.map(function(x){
      if(algo===0)return fd.fB[x]*1.1+Math.random()*3;
      if(algo===1)return (fd.n-fd.fB[x])*0.7+Math.random()*4;
      if(algo===2)return fd.fB[x]*0.5+Math.random()*5;
      if(algo===3)return fd.fB[x]*0.3+Math.random()*6;
      return fd.fB[x]*0.75+(fd.n-fd.fB[x])*0.25+Math.random()*5;
    });
    var f=pickByWeight(nf,wf,c.fCnt),b=pickByWeight(nb,wb,c.bCnt);
    var odds=f.filter(function(x){return x%2===1}).length;
    var fs=S(f),ideal=(c.fMax+1)*c.fCnt/2,span=f[f.length-1]-f[0];
    var z=[0,0,0];f.forEach(function(x){if(x<=c.zones[0])z[0]++;else if(x<=c.zones[0]+c.zones[1])z[1]++;else z[2]++});
    var sc=0;
    f.forEach(function(x){sc+=fd.fF[x]*(algo===1?-1:2)});b.forEach(function(x){sc+=fd.fB[x]*(algo===1?-1:2)});
    sc-=Math.abs(odds-c.fCnt/2)*(algo===2?20:10);
    sc-=Math.abs(fs-ideal)*(algo===3?0.75:0.25);
    sc-=Math.abs(span-(c.fMax*0.62))*(algo===3?1.8:0.25);
    if(algo===2)sc-=Math.abs(z[0]-z[1])*8+Math.abs(z[1]-z[2])*8;
    if(sc>bestS){bestS=sc;bestF=f.slice();bestB=b.slice()}
  }
  return {f:bestF,b:bestB,algo:algo,name:ALGO_NAMES[algo]};
}

// COUNTDOWN
function getNextDraw(c){
  var now=new Date(),dow=now.getDay(),minD=99;
  for(var i=0;i<c.dd.length;i++){
    var diff=(c.dd[i]-dow+7)%7;
    if(diff===0){var dt=new Date(now);dt.setHours(c.dh,c.dm,0,0);if(now>=dt)diff=7}
    if(diff<minD)minD=diff;
  }
  var nd=new Date(now);nd.setDate(nd.getDate()+minD);nd.setHours(c.dh,c.dm,0,0);
  if(now>=nd)nd.setDate(nd.getDate()+7);
  return nd;
}

function updateCD(k){
  var c=CFG[k],next=getNextDraw(c),now=new Date();
  var diff=Math.max(0,Math.floor((next-now)/1000));
  var h=Math.floor(diff/3600),m=Math.floor((diff%3600)/60),s=diff%60;
  var el=document.getElementById(k+'-cd');if(!el)return;
  el.innerHTML=P(h)+'<span class="unit">时</span>'+P(m)+'<span class="unit">分</span>'+P(s)+'<span class="unit">秒</span>';
  var de=document.getElementById(k+'-cdate');if(!de)return;
  var wd=['日','一','二','三','四','五','六'];
  de.textContent=(next.getMonth()+1)+'月'+next.getDate()+'日 周'+wd[next.getDay()]+' '+c.dh+':'+P(c.dm)+'开奖';
}

// RENDER
function buildTab(k){
  var c=CFG[k],hist=k==='dlt'?DLT_HISTORY:SSQ_HISTORY;
  var latest=hist[0],np=GPN(latest.p,k);
  var ph=window._preds[k];
  ensurePred(k,np);
  renderTab(k,c,hist,ph);
}

function renderTab(k,c,hist,ph){
  var latest=hist[0],np=GPN(latest.p,k);
  var entry=ensurePred(k,np);
  var pred5=getPred5(k,c,hist,ph);
  var wd=['日','一','二','三','四','五','六'];
  var ddText='每周'+c.dd.map(function(d){return wd[d]}).join('/')+' '+c.dh+':'+P(c.dm);

  var h=[];
  h.push('<div class="header"><h1>'+c.name+'智能预测</h1><div class="sub">'+c.fLabel+'/'+c.bLabel+' | '+ddText+'</div></div>');

  // countdown
  h.push('<div class="ccard">');
  h.push('<div class="cl">即将开奖</div><div class="cp">第 <span id="'+k+'-npval">'+np+'</span> 期</div>');
  h.push('<div class="ct" id="'+k+'-cd">--:--:--</div>');
  h.push('<div class="cd" id="'+k+'-cdate"></div>');
  h.push('<div class="irow" style="margin-top:12px;margin-bottom:6px"><div class="sbox"><div class="sv" style="font-size:16px">'+drawPoolText(latest)+'</div><div class="sl">本期奖池</div></div><div class="sbox"><div class="sv" style="font-size:16px">'+drawSalesText(latest)+'</div><div class="sl">本期销量</div></div></div>');
  var online=isOnlineMode();
  h.push('<div class="fstat">模式: <span id="'+k+'-fs">'+(isAppMode()?'APP模式':(online?'联网模式':'本地模式'))+'</span><button class="sync-btn" data-action="fetchAPI" data-args="'+escHtmlAttr(k)+'">同步开奖</button></div>');
  h.push('<div class="cpred"><div class="cpred-t">本期五组预测 <button class="copy-btn" data-action="copyPred" data-args="'+escHtmlAttr(k)+','+escHtmlAttr(entry.p)+',true">复制五组</button></div>');
  entry.groups.forEach(function(g,idx){
    h.push('<div class="pred-line"><span class="pred-name">算法'+(idx+1)+'</span>');
    g.f.forEach(function(n){h.push('<span class="cball f">'+P(n)+'</span>')});
    h.push('<span style="display:inline-block;width:8px"></span>');
    g.b.forEach(function(n){h.push('<span class="cball b">'+P(n)+'</span>')});
    h.push('<button class="copy-btn" data-action="copyPred" data-args="'+escHtmlAttr(k)+','+escHtmlAttr(entry.p)+',false,'+idx+'">复制</button>');
    h.push('<div class="cpred-m">'+g.name+' · 和值'+S(g.f)+' · 跨度'+(g.f[g.f.length-1]-g.f[0])+'</div></div>');
  });
  h.push('</div></div>');

  // stats
  h.push('<div class="irow">');
  h.push('<div class="sbox"><div class="sv">'+ph.history.length+'</div><div class="sl">预测记录</div></div>');
  h.push('<div class="sbox"><div class="sv">'+hist.length+'</div><div class="sl">历史期数</div></div>');
  h.push('<div class="sbox"><div class="sv" id="'+k+'-avgf">--</div><div class="sl">均'+c.fLabel+'命中/'+c.fCnt+'</div></div>');
  h.push('<div class="sbox"><div class="sv" id="'+k+'-avgb">--</div><div class="sl">均'+c.bLabel+'命中/'+c.bCnt+'</div></div>');
  h.push('<div class="sbox"><div class="sv" id="'+k+'-best">--</div><div class="sl">最佳命中</div></div>');
  h.push('</div>');

  // 5 preds
  h.push('<div class="pcard"><div class="pcard-h"><span class="pcard-t pulse">未来5期预测</span><span class="pcard-r">第'+pred5[0].p+'期 - 第'+pred5[4].p+'期</span></div>');
  h.push('<div class="tw"><table><thead><tr><th>期号</th><th>'+c.fLabel+'</th><th>'+c.bLabel+'</th><th>和值</th><th>跨度</th><th>奇偶</th><th>区间</th><th>操作</th></tr></thead><tbody>');
  pred5.forEach(function(pr){
    var g=pr.groups[0],odds=g.f.filter(function(x){return x%2===1}).length;
    var zc=[0,0,0];
    g.f.forEach(function(x){if(x<=c.zones[0])zc[0]++;else if(x<=c.zones[0]+c.zones[1])zc[1]++;else zc[2]++});
    h.push('<tr><td style="color:#ffa500;font-weight:bold">'+pr.p+'</td><td>');
    g.f.forEach(function(n){h.push('<span class="pball f">'+P(n)+'</span>')});
    h.push('</td><td>');
    g.b.forEach(function(n){h.push('<span class="pball b">'+P(n)+'</span>')});
    h.push('</td><td>'+S(g.f)+'</td><td>'+(g.f[g.f.length-1]-g.f[0])+'</td><td>'+odds+'/'+(g.f.length-odds)+'</td><td>'+zc.join('/')+'</td>');
    h.push('<td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(k)+','+escHtmlAttr(pr.p)+'">展开</button></td></tr>');
    h.push('<tr class="pred-detail" id="'+k+'-detail-'+pr.p+'"><td colspan="8"><div class="pred-detail-box">');
    h.push('<div style="text-align:right;margin-bottom:6px"><button class="copy-btn" data-action="copyPred" data-args="'+escHtmlAttr(k)+','+escHtmlAttr(pr.p)+',true">复制本期五组</button></div>');
    pr.groups.forEach(function(gg,idx){
      h.push('<div class="pred-line"><span class="pred-name">算法'+(idx+1)+'</span>');
      gg.f.forEach(function(n){h.push('<span class="pball f">'+P(n)+'</span>')});
      h.push('<span style="display:inline-block;width:6px"></span>');
      gg.b.forEach(function(n){h.push('<span class="pball b">'+P(n)+'</span>')});
      h.push('<span class="cpred-m">'+gg.name+'</span></div>');
    });
    h.push('</div></td></tr>');
  });
  h.push('</tbody></table></div></div>');

  // accuracy
  h.push('<div class="acard"><div class="st" style="margin-top:0">预测历史与正确率 <span class="badge">'+ph.history.length+'条</span></div>');
  h.push(renderAcc(k,c,ph));
  h.push('</div>');

  // recent 10
  h.push('<div class="st">最近10期 <span class="badge">开奖结果</span></div>');
  h.push('<div class="tw"><table><thead><tr><th>期号</th><th>日期</th><th>'+c.fLabel+'</th><th>'+c.bLabel+'</th><th>销量</th><th>和值</th><th>奇偶</th><th>详情</th></tr></thead><tbody>');
  hist.slice(0,10).forEach(function(r){
    var odds=r.f.filter(function(x){return x%2===1}).length;
    h.push('<tr><td>'+r.p+'</td><td>'+r.d+'</td><td>');
    r.f.forEach(function(n){h.push('<span class="nball f">'+P(n)+'</span>')});
    h.push('</td><td>');
    r.b.forEach(function(n){h.push('<span class="nball b">'+P(n)+'</span>')});
    h.push('</td><td>'+drawSalesText(r)+'</td><td>'+S(r.f)+'</td><td>'+odds+'/'+(r.f.length-odds)+'</td><td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(k)+',draw-'+escHtmlAttr(r.p)+'">查看详情</button></td></tr>');
    h.push('<tr class="pred-detail" id="'+k+'-detail-draw-'+r.p+'"><td colspan="8">'+prizeRowsHtml(k,r)+'</td></tr>');
  });
  h.push('</tbody></table></div>');

  // random picker
  h.push('<div class="rand-card"><div class="rand-head">');
  h.push('<div class="rand-title">一键随机选号</div>');
  h.push('<div class="rand-ctrl"><span style="font-size:12px;color:#8899aa">生成组数</span><input type="number" id="'+k+'-rand-count" min="1" max="99" value="5">');
  h.push('<button class="btn btn-o" data-action="genRandomNums" data-args="'+escHtmlAttr(k)+'">随机生成</button>');
  h.push('<button class="copy-btn" data-action="copyRandomNums" data-args="'+escHtmlAttr(k)+'">复制</button>');
  h.push('<button class="copy-btn" data-action="clearRandomNums" data-args="'+escHtmlAttr(k)+'">清空</button></div></div>');
  h.push('<div class="rand-out" id="'+k+'-rand-out"><div style="text-align:center;color:#667788;font-size:12px;padding:8px">设置组数后点击随机生成</div></div></div>');

  // manual input
  h.push('<div class="icard"><h3>手动录入最新开奖</h3><div class="ig">');
  h.push('<input type="text" id="'+k+'-paste" placeholder="粘贴整组号码，如 01 02 03 04 05 + 06 07" style="min-width:220px"><button class="btn btn-o" data-action="fillMainPaste" data-args="'+escHtmlAttr(k)+'">填入</button>');
  h.push('<span style="color:#667788;font-size:12px">'+c.fLabel+'</span>');
  for(var i=0;i<c.fCnt;i++)h.push('<input type="number" id="'+k+'-inf'+i+'" min="1" max="'+c.fMax+'">');
  h.push('<span class="isep">+</span>');
  for(var i=0;i<c.bCnt;i++)h.push('<input type="number" id="'+k+'-inb'+i+'" min="1" max="'+c.bMax+'">');
  h.push('<button class="btn btn-o" data-action="submitR" data-args="'+escHtmlAttr(k)+'">确认</button></div></div>');

  // full history
  h.push('<div class="st" style="cursor:pointer" data-action="toggleH" data-args="'+escHtmlAttr(k)+'">完整历史数据 ▾</div><div id="'+k+'-hist" style="display:none"></div>');
  h.push('<div class="rule-card open"><div class="rule-head"><span>'+c.name+'奖级表</span><span>开奖后自动判奖</span></div><div class="rule-body">'+mainPrizeTableHtml(k)+'</div></div>');

  document.getElementById('tab-'+k).innerHTML=h.join('');
  updateCD(k);
}

function mainPrizeTableHtml(k){
  var rows=k==='ssq'?
  [['一等奖','红6+蓝1',mainPrizeMoney(k,1)],['二等奖','红6',mainPrizeMoney(k,2)],['三等奖','红5+蓝1',mainPrizeMoney(k,3)],['四等奖','红5或红4+蓝1',mainPrizeMoney(k,4)],['五等奖','红4或红3+蓝1',mainPrizeMoney(k,5)],['六等奖','蓝1',mainPrizeMoney(k,6)]]:
  [['一等奖','前5+后2',mainPrizeMoney(k,1)],['二等奖','前5+后1',mainPrizeMoney(k,2)],['三等奖','前5',mainPrizeMoney(k,3)],['四等奖','前4+后2',mainPrizeMoney(k,4)],['五等奖','前4+后1或前3+后2',mainPrizeMoney(k,5)],['六等奖','前4或前3+后1或前2+后2',mainPrizeMoney(k,6)],['七等奖','前3或前2+后1或前1+后2或后2',mainPrizeMoney(k,7)],['八等奖','前2或前1+后1或后1',mainPrizeMoney(k,8)]];
  var h=['<div class="tw" style="margin-top:8px"><table><thead><tr><th>奖级</th><th>中奖条件</th><th>中奖一组预计奖金</th></tr></thead><tbody>'];
  rows.forEach(function(r){h.push('<tr><td>'+r[0]+'</td><td>'+r[1]+'</td><td>'+r[2]+'</td></tr>')});
  h.push('</tbody></table></div>');
  return h.join('');
}

function getPred5(k,c,hist,ph){
  var base=hist[0].p,pred5=[];
  for(var i=0;i<5;i++){
    base=GPN(base,k);
    pred5.push(ensurePred(k,base));
  }
  return pred5;
}

function renderAcc(k,c,ph){
  var hist=ph.history,total=0,tfh=0,tbh=0,ex=0,gd=0,nm=0,pr=0,best=0,tt=0;
  hist.forEach(function(h){
    normalizeEntry(k,h);
    h.af=Array.isArray(h.af)?h.af:[];
    h.ab=Array.isArray(h.ab)?h.ab:[];
    h.f=Array.isArray(h.f)?h.f:[];
    h.b=Array.isArray(h.b)?h.b:[];
    h.groups=Array.isArray(h.groups)?h.groups:[];
    if(!h.checked)return;tt++;
    if(typeof h.fh!=='number')h.fh=h.af.filter(function(n){return h.f.indexOf(n)>=0}).length;
    if(typeof h.bh!=='number')h.bh=h.ab.filter(function(n){return h.b.indexOf(n)>=0}).length;
    total++;tfh+=h.fh;tbh+=h.bh;
    var th=h.fh+h.bh;best=Math.max(best,th);
    if(th>=c.fCnt+c.bCnt-1)ex++;else if(th>=c.fCnt+c.bCnt-2)gd++;else if(th>=1)nm++;else pr++;
  });
  if(tt>0){
    var elAvgf=document.getElementById(k+'-avgf'),elAvgb=document.getElementById(k+'-avgb'),elBest=document.getElementById(k+'-best');
    if(elAvgf)elAvgf.textContent=(tfh/tt).toFixed(1);
    if(elAvgb)elAvgb.textContent=(tbh/tt).toFixed(1);
    if(elBest)elBest.textContent=best+'/'+(c.fCnt+c.bCnt);
  }
  var h2=[];
  h2.push('<div class="ag">');
  h2.push('<div class="ai"><div class="av" style="color:#4ade80">'+ex+'</div><div class="al">优(≥'+(c.fCnt+c.bCnt-1)+')</div></div>');
  h2.push('<div class="ai"><div class="av" style="color:#fbbf24">'+gd+'</div><div class="al">良好(≥'+(c.fCnt+c.bCnt-2)+')</div></div>');
  h2.push('<div class="ai"><div class="av" style="color:#fb923c">'+nm+'</div><div class="al">一般(≥1)</div></div>');
  h2.push('<div class="ai"><div class="av" style="color:#f87171">'+pr+'</div><div class="al">差(0)</div></div></div>');
  if(hist.length===0){h2.push('<div style="text-align:center;color:#667788;padding:10px">暂无预测记录</div>');return h2.join('')}
  h2.push('<div class="tw" style="margin-top:10px"><table><thead><tr><th>期号</th><th>最佳预测</th><th>开奖</th><th>'+c.fLabel+'命中</th><th>'+c.bLabel+'命中</th><th>评级/奖级</th><th>操作</th></tr></thead><tbody>');
  hist.forEach(function(h){
    normalizeEntry(k,h);
    h.af=Array.isArray(h.af)?h.af:[];
    h.ab=Array.isArray(h.ab)?h.ab:[];
    h.f=Array.isArray(h.f)?h.f:[];
    h.b=Array.isArray(h.b)?h.b:[];
    h.groups=Array.isArray(h.groups)?h.groups:[];
    var th=h.fh+h.bh,badge='',bc='';
    if(th>=c.fCnt+c.bCnt-1){badge='优';bc='good'}
    else if(th>=c.fCnt+c.bCnt-2){badge='良好';bc='good'}
    else if(th>=1){badge='一般';bc='mid'}
    else{badge='差';bc='low'}
    var bg=h.bestGroup||1,show=(h.groupResults&&h.groupResults[bg-1])||{f:h.f,b:h.b,name:''};
    h2.push('<tr><td>'+h.p+'</td><td><div style="color:#ffa500;font-size:12px;margin-bottom:3px">算法'+bg+'</div>');
    (show.f||h.f).forEach(function(n){h2.push('<span class="pball f">'+P(n)+'</span>')});
    h2.push(' ');
    (show.b||h.b).forEach(function(n){h2.push('<span class="pball b">'+P(n)+'</span>')});
    h2.push('</td><td>');
    if(h.checked){
      h.af.forEach(function(n){h2.push('<span class="pball f">'+P(n)+'</span>')});
      h2.push(' ');
      h.ab.forEach(function(n){h2.push('<span class="pball b">'+P(n)+'</span>')});
    }else{h2.push('<span style="color:#667788">未开奖</span>')}
    var ptxt=h.checked?(h.prize?mainPrizeDisplay(k,h.prize):(h.prizeText||prizeName(h.prize))):'-';
    h2.push('</td><td>'+(h.checked?h.fh+'/'+c.fCnt:'-')+'</td><td>'+(h.checked?h.bh+'/'+c.bCnt:'-')+'</td>');
    h2.push('<td><span class="hb '+bc+'">'+badge+'</span><div style="margin-top:4px;color:'+(h.prize?'#4ade80':'#8899aa')+'">'+ptxt+'</div></td><td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(k)+',win-'+escHtmlAttr(h.p)+'">展开</button></td></tr>');
    h2.push('<tr class="pred-detail" id="'+k+'-detail-win-'+h.p+'"><td colspan="7">'+mainPrizeDetailHtml(k,h)+'</td></tr>');
  });
  h2.push('</tbody></table></div>');
  return h2.join('');
}

// HISTORY STORAGE
function loadH(k){try{return JSON.parse(localStorage.getItem(k+'_hist')||'[]')}catch(e){return[]}}
function saveH(k,h){try{localStorage.setItem(k+'_hist',JSON.stringify(h))}catch(e){}}
function loadP(k){try{var v=JSON.parse(localStorage.getItem(k+'_pending')||'[]');return Array.isArray(v)?v.filter(function(x){return x&&x.p}):[]}catch(e){return[]}}
function saveP(k,p){try{localStorage.setItem(k+'_pending',JSON.stringify(p))}catch(e){}}
function loadMainDraws(k){try{var v=JSON.parse(localStorage.getItem(k+'_draws')||'[]');return Array.isArray(v)?v:[]}catch(e){return[]}}
function saveMainDraws(k,h){try{localStorage.setItem(k+'_draws',JSON.stringify(h.slice(0,200)))}catch(e){}}
function mergeDrawHistory(base,stored){
  var map={},out=[];
  (stored||[]).concat(base||[]).forEach(function(r){if(r&&r.p&&!map[r.p]){map[r.p]=1;out.push(r)}});
  out.sort(function(a,b){return periodNum(b.p)-periodNum(a.p)});
  return out;
}
function initMainDrawHistory(){
  var dltStored=loadMainDraws('dlt'),ssqStored=loadMainDraws('ssq');
  DLT_HISTORY=mergeDrawHistory(DLT_HISTORY,dltStored);
  SSQ_HISTORY=mergeDrawHistory(SSQ_HISTORY,ssqStored);
  if(!dltStored.length)saveMainDraws('dlt',DLT_HISTORY);
  if(!ssqStored.length)saveMainDraws('ssq',SSQ_HISTORY);
}
function refreshMainPendingAfterDraw(k,p){
  var ph=window._preds[k],pn=periodNum(p);
  ph.pending=ph.pending.filter(function(e){return e&&e.p&&periodNum(e.p)<pn});
  saveP(k,ph.pending);
}
function findP(ph,p){for(var i=0;i<ph.pending.length;i++){if(ph.pending[i]&&ph.pending[i].p===p)return ph.pending[i]}return null}
function normalizeEntry(k,e){
  if(!e)e={p:'',groups:[]};
  function ok(g){return g&&Array.isArray(g.f)&&Array.isArray(g.b)&&g.f.length===CFG[k].fCnt&&g.b.length===CFG[k].bCnt}
  var bad=!Array.isArray(e.groups)||e.groups.length!==5;
  if(!bad){for(var gi=0;gi<5;gi++){if(!ok(e.groups[gi])){bad=true;break}}}
  if(bad){
    var old=e.f&&e.b?{f:e.f.slice(),b:e.b.slice(),algo:0,name:ALGO_NAMES[0]}:null;
    // makeGroups现在是异步的，使用同步版本
    e.groups=[];
    for(var ai=0;ai<5;ai++){
      try{
        var pg=_predictSync(k,ai);
        e.groups.push({f:pg.f,b:pg.b,algo:ai,name:ALGO_NAMES[ai]});
      }catch(ex){}
    }
    if(old){e.groups[0]=old}
  }
  e.groups.forEach(function(g,i){if(!g.name)g.name=ALGO_NAMES[i]||('算法'+(i+1));g.algo=i});
  e.f=e.groups[0].f;e.b=e.groups[0].b;
  return e;
}
function ensurePred(k,p){
  var ph=window._preds[k],entry=findP(ph,p);
  if(entry){normalizeEntry(k,entry);saveP(k,ph.pending);return entry}
  // makeGroups现在是异步的
  var tempEntry={p:p,groups:[],checked:false,fh:0,bh:0};
  ph.pending.push(tempEntry);
  while(ph.pending.length>200)ph.pending.shift();
  saveP(k,ph.pending);
  // 异步生成预测组
  makeGroups(k).then(function(gs){
    var pendingEntry=findP(ph,p);
    if(pendingEntry){
      pendingEntry.groups=gs;
      normalizeEntry(k,pendingEntry);
      saveP(k,ph.pending);
    }
  }).catch(function(e){console.error('makeGroups error:',e)});
  return tempEntry;
}
function fmtGroup(g){return g.f.map(P).join(' ')+' + '+g.b.map(P).join(' ')}
function predText(k,p,all,idx){
  var e=findP(window._preds[k],p);if(!e)return '';
  normalizeEntry(k,e);
  if(all)return e.p+'期\n'+e.groups.map(function(g,i){return '算法'+(i+1)+' '+g.name+': '+fmtGroup(g)}).join('\n');
  var g=e.groups[idx||0];return e.p+'期 算法'+((idx||0)+1)+' '+g.name+': '+fmtGroup(g);
}
function copyText(t){
  if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(t).then(function(){alert('已复制号码')}).catch(function(){fallbackCopy(t)})}
  else fallbackCopy(t);
}
function fallbackCopy(t){var ta=document.createElement('textarea');ta.value=t;document.body.appendChild(ta);ta.select();document.execCommand('copy');document.body.removeChild(ta);alert('已复制号码')}
function copyPred(k,p,all,idx){copyText(predText(k,p,all,idx))}
function togglePredDetail(k,p){
  var el=document.getElementById(k+'-detail-'+p);if(!el)return;
  el.classList.toggle('show');
}
function prizeName(level){return level?('第'+level+'等奖'):'未中奖'}
function prizeMoneyText(v){return v?('预计 '+v):''}
function mainPrizeMoney(k,level){
  if(!level)return '';
  var m=k==='ssq'?{1:'浮动，以开奖为准',2:'浮动，以开奖为准',3:'3,000元',4:'200元',5:'10元',6:'5元'}:
  {1:'浮动，以开奖为准',2:'浮动，以开奖为准',3:'10,000元',4:'3,000元',5:'300元',6:'200元',7:'100元',8:'15元',9:'5元'};
  return m[level]||'以开奖为准';
}
function mainPrizeDisplay(k,level){
  if(!level)return '未中奖';
  return prizeName(level)+' · '+prizeMoneyText(mainPrizeMoney(k,level));
}
function lotteryPrizeMoney(id,level){
  if(!level)return '';
  var maps={
    qlc:{1:'浮动，以开奖为准',2:'浮动，以开奖为准',3:'浮动，以开奖为准',4:'200元',5:'50元',6:'10元',7:'5元'},
    fc3d:{1:'1,040元',2:'346元',3:'173元'},
    pl3:{1:'1,040元',2:'346元',3:'173元'},
    pl5:{1:'100,000元'},
    qxc:{1:'浮动，以开奖为准',2:'浮动，以开奖为准',3:'3,000元',4:'500元',5:'30元',6:'5元'},
    kl8:{1:'浮动，以开奖为准',2:'8,000元',3:'720元',4:'80元',5:'5元',6:'3元',7:'2元'}
  };
  return (maps[id]&&maps[id][level])||'以开奖为准';
}
function lotteryPrizeDisplay(id,level){
  if(!level)return '未中奖';
  return level+'等奖 · '+prizeMoneyText(lotteryPrizeMoney(id,level));
}
function judgeMainPrize(k,fh,bh){
  if(k==='ssq'){
    if(fh===6&&bh===1)return 1;
    if(fh===6)return 2;
    if(fh===5&&bh===1)return 3;
    if(fh===5||fh===4&&bh===1)return 4;
    if(fh===4||fh===3&&bh===1)return 5;
    if(bh===1)return 6;
    return 0;
  }
  if(fh===5&&bh===2)return 1;
  if(fh===5&&bh===1)return 2;
  if(fh===5&&bh===0)return 3;
  if(fh===4&&bh===2)return 4;
  if((fh===4&&bh===1)||(fh===3&&bh===2))return 5;
  if((fh===4&&bh===0)||(fh===3&&bh===1)||(fh===2&&bh===2))return 6;
  if((fh===3&&bh===0)||(fh===2&&bh===1)||(fh===1&&bh===2)||(fh===0&&bh===2))return 7;
  if((fh===2&&bh===0)||(fh===1&&bh===1)||(fh===0&&bh===1))return 8;
  return 0;
}
function bestMainPrize(k,entry,af,ab){
  normalizeEntry(k,entry);
  var best={level:0,idx:0,fh:0,bh:0,groups:[]};
  entry.groups.forEach(function(g,idx){
    var fh=af.filter(function(n){return g.f.indexOf(n)>=0}).length;
    var bh=ab.filter(function(n){return g.b.indexOf(n)>=0}).length;
    var level=judgeMainPrize(k,fh,bh);
    var one={idx:idx+1,name:g.name,f:g.f.slice(),b:g.b.slice(),fh:fh,bh:bh,level:level};
    best.groups.push(one);
    if((level&&(!best.level||level<best.level))||(!best.level&&fh+bh>best.fh+best.bh))best={level:level,idx:idx,fh:fh,bh:bh,groups:best.groups};
  });
  return best;
}
function mainPrizeDetailHtml(k,h){
  var rows=h.groupResults||[];
  if(!rows.length&&h.groups){
    rows=h.groups.map(function(g,idx){return {idx:idx+1,name:g.name,f:g.f||[],b:g.b||[],fh:0,bh:0,level:0}});
  }
  var out=['<div class="pred-detail-box"><div style="color:#ffa500;margin-bottom:6px">五组预测中奖状态</div>'];
  rows.forEach(function(r){
    out.push('<div class="pred-line"><span class="pred-name">算法'+r.idx+'</span>');
    (r.f||[]).forEach(function(n){out.push('<span class="pball f">'+P(n)+'</span>')});
    out.push('<span style="display:inline-block;width:6px"></span>');
    (r.b||[]).forEach(function(n){out.push('<span class="pball b">'+P(n)+'</span>')});
    out.push('<span class="cpred-m">'+(r.name||'')+' · 命中 '+r.fh+'+'+r.bh+' · '+mainPrizeDisplay(k,r.level)+'</span></div>');
  });
  out.push('</div>');
  return out.join('');
}

// RANDOM PICKER
window._randoms={dlt:[],ssq:[]};
function randomSet(max,cnt){
  var a=[];for(var i=1;i<=max;i++)a.push(i);
  SF(a);return a.slice(0,cnt).sort(function(x,y){return x-y});
}
function genRandomNums(k){
  var c=CFG[k],inp=document.getElementById(k+'-rand-count'),n=parseInt(inp&&inp.value)||1;
  n=Math.max(1,Math.min(99,n));if(inp)inp.value=n;
  var arr=[];
  for(var i=0;i<n;i++){arr.push({f:randomSet(c.fMax,c.fCnt),b:randomSet(c.bMax,c.bCnt)})}
  window._randoms[k]=arr;renderRandomNums(k);
}
function renderRandomNums(k){
  var out=document.getElementById(k+'-rand-out'),arr=window._randoms[k]||[];
  if(!out)return;
  if(arr.length===0){out.innerHTML='<div style="text-align:center;color:#667788;font-size:12px;padding:8px">暂无随机号码</div>';return}
  var h=[];
  arr.forEach(function(g,i){
    h.push('<div class="rand-line"><span class="rand-idx">'+(i+1)+'组</span>');
    g.f.forEach(function(n){h.push('<span class="pball f">'+P(n)+'</span>')});
    h.push('<span style="display:inline-block;width:6px"></span>');
    g.b.forEach(function(n){h.push('<span class="pball b">'+P(n)+'</span>')});
    h.push('</div>');
  });
  out.innerHTML=h.join('');
}
function randomText(k){
  var arr=window._randoms[k]||[];
  if(arr.length===0)return '';
  return arr.map(function(g,i){return (i+1)+'组 '+g.f.map(P).join(' ')+' + '+g.b.map(P).join(' ')}).join('\n');
}
function copyRandomNums(k){
  var t=randomText(k);
  if(!t){alert('请先随机生成号码');return}
  copyText(t);
}
function clearRandomNums(k){
  window._randoms[k]=[];
  renderRandomNums(k);
}

function pasteNums(txt,len,digits){
  var raw=String(txt||'').trim(),nums=[];
  if(digits&&raw.replace(/\D/g,'').length>=len&&raw.match(/\d+/g)&&raw.match(/\d+/g).length===1){
    var s=raw.replace(/\D/g,'').slice(0,len);
    for(var i=0;i<s.length;i++)nums.push(parseInt(s.charAt(i),10));
    return nums;
  }
  var m=raw.match(/\d+/g)||[];
  for(var j=0;j<m.length&&nums.length<len;j++)nums.push(parseInt(m[j],10));
  return nums;
}
function fillMainPaste(k){
  var c=CFG[k],inp=document.getElementById(k+'-paste'),nums=pasteNums(inp&&inp.value,c.fCnt+c.bCnt,false);
  if(nums.length<c.fCnt+c.bCnt){alert('号码数量不够，请检查粘贴内容');return}
  for(var i=0;i<c.fCnt;i++)document.getElementById(k+'-inf'+i).value=nums[i];
  for(var j=0;j<c.bCnt;j++)document.getElementById(k+'-inb'+j).value=nums[c.fCnt+j];
}
function fmtMoney(v){
  if(v===undefined||v===null||v==='')return '同步后显示';
  var s=String(v);
  if(/[亿万元]/.test(s))return s;
  var n=parseFloat(s.replace(/,/g,''));if(isNaN(n))return s;
  return Math.round(n).toLocaleString('zh-CN')+'元';
}
function drawPoolText(r){return fmtMoney(r&&r.pool)}
function drawSalesText(r){return fmtMoney(r&&r.sales)}
function defaultPrizeRows(kind){
  var rows={
    dlt:['一等奖','二等奖','三等奖','四等奖','五等奖','六等奖','七等奖','八等奖','九等奖'],
    ssq:['一等奖','二等奖','三等奖','四等奖','五等奖','六等奖'],
    qlc:['一等奖','二等奖','三等奖','四等奖','五等奖','六等奖','七等奖'],
    fc3d:['直选','组选3','组选6'],
    pl3:['直选','组选3','组选6'],
    pl5:['一等奖'],
    qxc:['一等奖','二等奖','三等奖','四等奖','五等奖','六等奖'],
    kl8:['选十中十','选十中九','选十中八','选十中七','选十中六','选十中五','选十中零']
  }[kind]||['一等奖','二等奖','三等奖'];
  return rows.map(function(x){return {type:x,num:'同步后显示',money:'同步后显示'}});
}
function prizeRowsHtml(kind,r){
  var rows=(r&&r.grades&&r.grades.length?r.grades:defaultPrizeRows(kind));
  var h=['<div class="tw"><table><thead><tr><th>奖项</th><th>注数</th><th>奖金</th></tr></thead><tbody>'];
  rows.forEach(function(x){h.push('<tr><td>'+(x.type||x.name||'奖项')+'</td><td>'+(x.num||x.typenum||'同步后显示')+'</td><td>'+fmtMoney(x.money||x.typemoney)+'</td></tr>')});
  h.push('</tbody></table></div>');
  return h.join('');
}

// MANUAL SUBMIT
function submitR(k){
  var c=CFG[k],hist=k==='dlt'?DLT_HISTORY:SSQ_HISTORY,f=[],b=[];
  for(var i=0;i<c.fCnt;i++){var v=parseInt(document.getElementById(k+'-inf'+i).value);if(v>=1&&v<=c.fMax)f.push(v)}
  for(var i=0;i<c.bCnt;i++){var v=parseInt(document.getElementById(k+'-inb'+i).value);if(v>=1&&v<=c.bMax)b.push(v)}
  if(f.length!==c.fCnt||b.length!==c.bCnt){alert('请填写完整号码');return}
  if(hasDup(f)||hasDup(b)){alert('号码不能重复');return}
  f.sort(function(a,b){return a-b});b.sort(function(a,b){return a-b});
  var np=GPN(hist[0].p,k);np=prompt('期号',np);if(!np)return;
  var ph=window._preds[k],pending=ph.pending;
  for(var i=0;i<pending.length;i++){
    if(pending[i].p===np&&!pending[i].checked){
      pending[i].checked=true;pending[i].af=f;pending[i].ab=b;
      normalizeEntry(k,pending[i]);
      var bp=bestMainPrize(k,pending[i],f,b);
      pending[i].fh=bp.fh;pending[i].bh=bp.bh;pending[i].bestGroup=bp.idx+1;pending[i].prize=bp.level;pending[i].prizeText=prizeName(bp.level);pending[i].groupResults=bp.groups;
      pending[i].cd=new Date().toISOString().split('T')[0];
      ph.history.unshift(pending[i]);saveH(k,ph.history);saveP(k,pending);break;
    }
  }
  renderTab(k,c,hist,ph);alert(c.name+' '+np+' 期已录入');
}

// TOGGLE FULL HISTORY
function toggleH(k){
  var el=document.getElementById(k+'-hist'),hist=k==='dlt'?DLT_HISTORY:SSQ_HISTORY;
  if(el.style.display==='none'||el.style.display===''){
    var h=['<div class="tw"><table><thead><tr><th>期号</th><th>日期</th><th>号码</th><th>销量</th><th>和值</th><th>详情</th></tr></thead><tbody>'];
    hist.forEach(function(r){
      h.push('<tr><td>'+r.p+'</td><td>'+r.d+'</td><td>');
      r.f.forEach(function(n){h.push('<span class="nball f">'+P(n)+'</span>')});
      h.push(' ');
      r.b.forEach(function(n){h.push('<span class="nball b">'+P(n)+'</span>')});
      h.push('</td><td>'+drawSalesText(r)+'</td><td>'+S(r.f)+'</td><td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(k)+',full-'+escHtmlAttr(r.p)+'">查看详情</button></td></tr>');
      h.push('<tr class="pred-detail" id="'+k+'-detail-full-'+r.p+'"><td colspan="6">'+prizeRowsHtml(k,r)+'</td></tr>');
    });
    h.push('</tbody></table></div>');
    el.innerHTML=h.join('');el.style.display='block';
  }else{el.style.display='none'}
}

function handleLatest(k,d){
  var c=CFG[k],hist=k==='dlt'?DLT_HISTORY:SSQ_HISTORY,ph=window._preds[k];
  if(d&&d.period&&d.front&&d.back){
    d.period=String(d.period);
    var same=hist[0]&&hist[0].p===d.period;
    var topP=hist[0]&&hist[0].p?String(hist[0].p):'';
    var newer=!topP||parseInt(d.period,10)>parseInt(topP,10);
    var statusText='已同步开奖、奖池和销量';
    if(!same&&newer){
      var pending=ph.pending;
      for(var i=0;i<pending.length;i++){
        if(pending[i].p===d.period&&!pending[i].checked){
          pending[i].checked=true;pending[i].af=d.front;pending[i].ab=d.back;
          normalizeEntry(k,pending[i]);
          var bp=bestMainPrize(k,pending[i],d.front,d.back);
          pending[i].fh=bp.fh;pending[i].bh=bp.bh;pending[i].bestGroup=bp.idx+1;pending[i].prize=bp.level;pending[i].prizeText=prizeName(bp.level);pending[i].groupResults=bp.groups;
          pending[i].cd=d.date||'';ph.history.unshift(pending[i]);saveH(k,ph.history);saveP(k,pending);break;
        }
      }
      hist=hist.filter(function(x){return x&&x.p!==d.period});
      hist.unshift({p:d.period,d:d.date||'',f:d.front,b:d.back,sales:d.sales||'',pool:d.pool||'',grades:d.grades||[]});
      if(k==='dlt')DLT_HISTORY=hist;else SSQ_HISTORY=hist;
      saveMainDraws(k,hist);
      refreshMainPendingAfterDraw(k,d.period);
      renderTab(k,c,hist,ph);
      statusText='已更新到 '+d.period+' 期';
    }else if(same){
      hist[0].f=d.front.slice();
      hist[0].b=d.back.slice();
      hist[0].sales=d.sales||hist[0].sales||'';
      hist[0].pool=d.pool||hist[0].pool||'';
      hist[0].grades=d.grades||hist[0].grades||[];
      if(d.date)hist[0].d=d.date;
      saveMainDraws(k,hist);
      renderTab(k,c,hist,ph);
      statusText='已刷新 '+d.period+' 期开奖号码';
    }else{
      var updated=false;
      for(var hi=0;hi<hist.length;hi++){
        if(hist[hi]&&hist[hi].p===d.period){
          hist[hi].f=d.front.slice();
          hist[hi].b=d.back.slice();
          hist[hi].sales=d.sales||hist[hi].sales||'';
          hist[hi].pool=d.pool||hist[hi].pool||'';
          hist[hi].grades=d.grades||hist[hi].grades||[];
          if(d.date)hist[hi].d=d.date;
          updated=true;break;
        }
      }
      if(updated)saveMainDraws(k,hist);
      renderTab(k,c,hist,ph);
      statusText='接口返回 '+d.period+' 期，本地最新为 '+topP+' 期';
    }
    var fs=document.getElementById(k+'-fs');if(fs){fs.textContent=statusText;fs.style.color=newer||same?'#4ade80':'#fbbf24'}
  }else{var fs2=document.getElementById(k+'-fs');if(fs2){fs2.textContent='无新数据';fs2.style.color='#667788'}}
}

function fromSsqCwl(j){
  var item=j&&j.result&&j.result[0];if(!item)return null;
  var red=parseNums(item.red,6),blue=parseNums(item.blue,1);
  if(red.length<6||blue.length<1)return null;
  return {period:item.code,date:normalizeRemoteDate(item.date),front:red,back:blue,sales:item.sales||'',pool:item.poolmoney||'',grades:(item.prizegrades||[]).map(function(g){return {type:g.type,num:g.typenum,money:g.typemoney}})};
}
function fetchSsqDirect(){
  return fetchJson('https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=1').then(function(j){var d=fromSsqCwl(j);if(!d)throw 'empty ssq';return d}).catch(function(){return fetchEastmoneyDirect('ssq')});
}
function fetchDltDirect(){
  return fetchShSportsDltDirect().catch(function(){return fetchEastmoneyDirect('dlt')});
}
function fetchMainDirect(k){
  if(k==='ssq')return fetchSsqDirect();
  if(k==='dlt')return fetchDltDirect();
  return Promise.reject('unknown main lottery');
}

// SYNC / LOCAL MODE
function fetchAPI(k){
  var c=CFG[k],hist=k==='dlt'?DLT_HISTORY:SSQ_HISTORY,ph=window._preds[k];
  if(isAppMode()){
    var appFs=document.getElementById(k+'-fs');if(appFs){appFs.textContent='同步中...';appFs.style.color='#fbbf24'}
    try{
      var raw=AndroidSync.fetchLatest(k),data=JSON.parse(raw||'[]'),d0=Array.isArray(data)?data[0]:data;
      if(!d0||!d0.period)throw 'empty app data';
      handleLatest(k,d0);
    }catch(e){
      fetchMainDirect(k).then(function(d){handleLatest(k,d)}).catch(function(){
        appFs=document.getElementById(k+'-fs');if(appFs){appFs.textContent='同步失败，数据源暂不可用';appFs.style.color='#f87171'}
      });
    }
    return;
  }
  if(!canUseLocalApi()){
    var localFs=document.getElementById(k+'-fs');
    if(localFs){localFs.textContent='联网同步中...';localFs.style.color='#fbbf24'}
    fetchMainDirect(k).then(function(d){
      handleLatest(k,d);
    }).catch(function(){
      renderTab(k,c,hist,ph);
      localFs=document.getElementById(k+'-fs');
      if(localFs){localFs.textContent='无法联网采集，已使用本地数据';localFs.style.color='#f87171'}
    });
    return;
  }
  var fs=document.getElementById(k+'-fs');if(fs){fs.textContent='同步中...';fs.style.color='#fbbf24'}
  fetch(CFG[k].api).then(function(r){return r.json()}).then(function(data){
    handleLatest(k,Array.isArray(data)?data[0]:data);
  }).catch(function(e){
    fetchMainDirect(k).then(function(d){handleLatest(k,d)}).catch(function(){
      fs=document.getElementById(k+'-fs');if(fs){fs.textContent='同步失败，数据源暂不可用';fs.style.color='#f87171'}
    });
  });
}

// DELTA CALCULATOR (no external deps)
function deltaCalc(){
  var c=parseFloat(document.getElementById('dpureCoin').value)||0,r=parseFloat(document.getElementById('dratio').value)||40;
  var awm=parseInt(document.getElementById('dawm').value)||0,awmP=parseFloat(document.getElementById('dawmP').value)||0;
  var red=parseInt(document.getElementById('dred').value)||0,redP=parseFloat(document.getElementById('dredP').value)||0;
  var helm=parseInt(document.getElementById('dhelm').value)||0,helmP=parseFloat(document.getElementById('dhelmP').value)||0;
  var arm=parseInt(document.getElementById('darmor').value)||0,armP=parseFloat(document.getElementById('darmorP').value)||0;
  var pv=c*100/r,aw=awm*awmP,rv=red*redP,hv=helm*helmP,av=arm*armP,tv=pv+aw+rv+hv+av;
  document.getElementById('drPure').textContent=pv.toFixed(2)+' 元';
  document.getElementById('drAWM').textContent=aw.toFixed(2)+' 元';
  document.getElementById('drRed').textContent=rv.toFixed(2)+' 元';
  document.getElementById('drHelm').textContent=hv.toFixed(2)+' 元';
  document.getElementById('drArmor').textContent=av.toFixed(2)+' 元';
  document.getElementById('drTotal').textContent=tv.toFixed(2)+' 元';
  var d='';
  if(c>0)d+='纯币: '+c+'M x 100 / '+r+' = '+pv.toFixed(2)+'元\n';
  if(awm>0)d+='AWM: '+awm+'个 x '+awmP.toFixed(2)+' = '+aw.toFixed(2)+'元\n';
  if(red>0)d+='红弹: '+red+'个 x '+redP.toFixed(2)+' = '+rv.toFixed(2)+'元\n';
  if(helm>0)d+='头盔: '+helm+'个 x '+helmP.toFixed(2)+' = '+hv.toFixed(2)+'元\n';
  if(arm>0)d+='护甲: '+arm+'个 x '+armP.toFixed(2)+' = '+av.toFixed(2)+'元\n';
  if(d)d+='\n总计: '+pv.toFixed(2)+'+'+aw.toFixed(2)+'+'+rv.toFixed(2)+'+'+hv.toFixed(2)+'+'+av.toFixed(2)+'='+tv.toFixed(2)+'元';
  else d='请至少输入一项数据';
  document.getElementById('drDetail').textContent=d;
}
function deltaReset(){
  ['dpureCoin','dawm','dawmP','dred','dredP','dhelm','dhelmP','darmor','darmorP'].forEach(function(id){document.getElementById(id).value=''});
  document.getElementById('dratio').value='40';
  ['drPure','drAWM','drRed','drHelm','drArmor','drTotal'].forEach(function(id){document.getElementById(id).textContent='0.00 元'});
  document.getElementById('drDetail').textContent='输入数据后点击"计算总价"';
}
function deltaDemo(){
  document.getElementById('dpureCoin').value='100';
  document.getElementById('dratio').value='40';
  document.getElementById('dawm').value='100';document.getElementById('dawmP').value='0.8';
  document.getElementById('dred').value='100';document.getElementById('dredP').value='0.15';
  document.getElementById('dhelm').value='10';document.getElementById('dhelmP').value='1.5';
  document.getElementById('darmor').value='10';document.getElementById('darmorP').value='1.5';
  deltaCalc();
}

// LOTTERY PAGES
window._lotteryPicks={};
window._lotteryMany={};
function lotById(id){return LOTTERIES.filter(function(x){return x.id===id})[0]}
function lotNextPeriod(p){
  var s=String(p||'00001'),n=parseInt(s.replace(/\D/g,''))||0;
  return LP(n+1,s.length);
}
function lotPick(lot){
  if(lot.mode==='balls'){
    return lot.sets.map(function(s){return {cls:s.cls,nums:randomSet(s.max,s.cnt)}});
  }
  if(lot.mode==='digits'){
    var ds=[];for(var i=0;i<lot.len;i++)ds.push(R(10)-1);
    return [{cls:'d',nums:ds}];
  }
  var os=[];for(var j=0;j<lot.cnt;j++)os.push(lot.opts[Math.floor(Math.random()*lot.opts.length)]);
  return [{cls:'o',nums:os}];
}
function lotPickText(lot,pick){
  return pick.map(function(set){
    if(set.cls==='d')return set.nums.join('');
    if(set.cls==='o')return set.nums.join(' ');
    return set.nums.map(P).join(' ');
  }).join(' + ');
}
function lotPickHtml(pick,small){
  var cls=small?'pball':'cball',h=[];
  pick.forEach(function(set,si){
    if(si>0)h.push('<span style="display:inline-block;width:6px"></span>');
    set.nums.forEach(function(n){
      if(set.cls==='f'||set.cls==='b')h.push('<span class="'+cls+' '+set.cls+'">'+P(n)+'</span>');
      else h.push('<span class="lot-tag">'+n+'</span>');
    });
  });
  return h.join('');
}
function lotMakeGroups(lot){
  var gs=[];for(var i=0;i<5;i++)gs.push({name:ALGO_NAMES[i],pick:lotPick(lot)});
  return gs;
}
function lotLoad(id,key){try{var v=JSON.parse(localStorage.getItem(id+'_lot_'+key)||'[]');return Array.isArray(v)?v:[]}catch(e){return[]}}
function lotSave(id,key,v){try{localStorage.setItem(id+'_lot_'+key,JSON.stringify(v))}catch(e){}}
function lotPickKey(pick){
  if(!pick||!pick.length)return '';
  return pick.map(function(s){return s.cls+':'+s.nums.join(',')}).join('|');
}
function lotDefaultHistory(id){
  var d=LOT_DEFAULT_HISTORY[id]||[];
  return JSON.parse(JSON.stringify(d));
}
function lotHistory(id){
  var h=lotLoad(id,'history'),def=lotDefaultHistory(id);
  if(def.length){
    var map={},merged=[];
    h=h.filter(function(x){return x&&x.p&&x.p!=='00001'&&x.p!=='00002'});
    (h.length?h:def).concat(def).forEach(function(x){if(x&&x.p&&!map[x.p]){map[x.p]=1;merged.push(x)}});
    if(JSON.stringify(merged)!==JSON.stringify(h)){h=merged;if(!lotLoad(id,'history').length)lotSave(id,'history',h)}
    var pending=lotPending(id).filter(function(x){return x&&x.p&&x.p!=='00001'&&x.p!=='00002'});
    if(!lotLoad(id,'pending').length&&pending.length)lotSave(id,'pending',pending);
  }
  if(h.length===0){h=[{p:'00001',d:new Date().toISOString().split('T')[0],pick:lotPick(lotById(id))}];lotSave(id,'history',h)}
  return h;
}
function lotPending(id){return lotLoad(id,'pending').filter(function(x){return x&&x.p&&x.p!=='00001'&&x.p!=='00002'})}
function lotFindPending(arr,p){for(var i=0;i<arr.length;i++){if(arr[i]&&arr[i].p===p)return arr[i]}return null}
function lotEnsureEntry(id,p){
  var lot=lotById(id),pending=lotPending(id),e=lotFindPending(pending,p);
  if(e&&Array.isArray(e.groups)&&e.groups.length===5)return e;
  e={p:p,groups:lotMakeGroups(lot),checked:false};
  pending.push(e);while(pending.length>200)pending.shift();
  lotSave(id,'pending',pending);
  return e;
}
function lotGetPred5(id){
  var hist=lotHistory(id),base=hist[0].p,out=[];
  for(var i=0;i<5;i++){base=lotNextPeriod(base);out.push(lotEnsureEntry(id,base))}
  return out;
}
function lotActualFromData(lot,d){
  if(!d)return null;
  if(d.pick)return d.pick;
  if(d.front||d.back){
    if(lot.mode==='digits')return [{cls:'d',nums:(d.front||[]).concat(d.back||[]).slice(0,lot.len)}];
    var pick=[];
    if(d.front)pick.push({cls:'f',nums:d.front});
    if(d.back&&d.back.length)pick.push({cls:'b',nums:d.back});
    return pick;
  }
  if(d.nums)return [{cls:lot.mode==='digits'?'d':'f',nums:d.nums}];
  return null;
}
function lotDrawInfo(lot){
  var txt=lot.draw||'',days=null,h=20,m=40,mm=txt.match(/(\d{1,2}):(\d{2})/);
  if(mm){h=parseInt(mm[1]);m=parseInt(mm[2])}
  if(txt.indexOf('每天')>=0)days=[0,1,2,3,4,5,6];
  else{
    days=[];var map={'日':0,'一':1,'二':2,'三':3,'四':4,'五':5,'六':6};
    for(var k in map){if(txt.indexOf(k)>=0)days.push(map[k])}
    if(!days.length)days=[0,1,2,3,4,5,6];
  }
  return {days:days,h:h,m:m};
}
function lotNextDraw(lot){
  var info=lotDrawInfo(lot),now=new Date(),best=null;
  for(var i=0;i<8;i++){
    var d=new Date(now.getFullYear(),now.getMonth(),now.getDate()+i,info.h,info.m,0);
    var wd=d.getDay();
    var ok=false;for(var j=0;j<info.days.length;j++){if(info.days[j]===wd)ok=true}
    if(ok&&d>now&&(!best||d<best))best=d;
  }
  return best||new Date(now.getTime()+86400000);
}
function lotLastDraw(lot){
  var info=lotDrawInfo(lot),now=new Date(),best=null;
  for(var i=0;i<8;i++){
    var d=new Date(now.getFullYear(),now.getMonth(),now.getDate()-i,info.h,info.m,0);
    var wd=d.getDay(),ok=false;
    for(var j=0;j<info.days.length;j++){if(info.days[j]===wd)ok=true}
    if(ok&&d<=now&&(!best||d>best))best=d;
  }
  return best;
}
function updateLotteryCD(id){
  var lot=lotById(id),cd=document.getElementById(id+'-lot-cd'),dt=document.getElementById(id+'-lot-cdate');
  if(!lot||!cd)return;
  var next=lotNextDraw(lot),now=new Date(),diff=Math.max(0,Math.floor((next-now)/1000));
  var d=Math.floor(diff/86400),h=Math.floor(diff%86400/3600),m=Math.floor(diff%3600/60),s=diff%60;
  cd.textContent=d+'天 '+LP(h,2)+'时 '+LP(m,2)+'分 '+LP(s,2)+'秒';
  if(dt)dt.textContent='开奖时间：'+(lot.draw||'以官方公告为准')+' · 下期开奖 '+next.toLocaleString('zh-CN');
  var last=lotLastDraw(lot);
  if(last&&now-last>=0&&now-last<=7200000)autoFetchAfterDraw(id,last);
}
function updateAllLotteryCD(){
  for(var i=0;i<LOTTERIES.length;i++){
    var id=LOTTERIES[i].id;
    if(id!=='dlt'&&id!=='ssq')updateLotteryCD(id);
  }
}
function lotMetricNums(pick){
  if(!pick||!pick.length)return [];
  var nums=(pick[0].nums||[]).slice();
  return nums;
}
function lotMetricText(lot,pick,type){
  var nums=lotMetricNums(pick),sum=S(nums),od=0,zc=[0,0,0],max=lot.mode==='digits'?9:(lot.sets&&lot.sets[0]?lot.sets[0].max:80);
  for(var i=0;i<nums.length;i++){
    if(nums[i]%2===1)od++;
    if(nums[i]<=Math.ceil(max/3))zc[0]++;else if(nums[i]<=Math.ceil(max*2/3))zc[1]++;else zc[2]++;
  }
  if(type==='sum')return sum;
  if(type==='span'){if(!nums.length)return 0;var mn=nums[0],mx=nums[0];for(var j=1;j<nums.length;j++){if(nums[j]<mn)mn=nums[j];if(nums[j]>mx)mx=nums[j]}return mx-mn}
  if(type==='odd')return od+'/'+(nums.length-od);
  if(type==='zone')return zc.join('/');
  return '';
}
function lotHistoryAccuracyHtml(id,lot){
  var arr=lotPrizeLoad(id),win=0,best='未中奖';
  arr.forEach(function(x){if(x.level){win++;if(best==='未中奖'||x.level<parseInt(best))best=x.level+'等奖'}});
  var h=['<div class="acard"><div class="st" style="margin-top:0">预测历史与中奖结果 <span class="badge">'+arr.length+'条</span></div>'];
  h.push('<div class="ag"><div class="ai"><div class="av" style="color:#4ade80">'+win+'</div><div class="al">中奖期数</div></div><div class="ai"><div class="av" style="color:#fbbf24">'+(arr.length-win)+'</div><div class="al">未中奖</div></div><div class="ai"><div class="av" style="color:#fb923c">'+best+'</div><div class="al">最高奖级</div></div><div class="ai"><div class="av" style="color:#f87171">'+arr.length+'</div><div class="al">已核验</div></div></div>');
  if(!arr.length){h.push('<div style="text-align:center;color:#667788;padding:10px">暂无已核验预测记录，录入或同步新期开奖后将显示是否中奖</div></div>');return h.join('')}
  h.push('<div class="tw" style="margin-top:10px"><table><thead><tr><th>期号</th><th>最佳组</th><th>命中</th><th>中奖结果</th><th>开奖</th><th>操作</th></tr></thead><tbody>');
  arr.slice(0,20).forEach(function(r){h.push('<tr><td>'+r.p+'</td><td>算法'+r.bestGroup+'</td><td>'+r.hitText+'</td><td><span class="hb '+(r.level?'good':'low')+'">'+(r.level?r.level+'等奖':'未中奖')+'</span><div style="margin-top:4px;color:'+(r.level?'#4ade80':'#8899aa')+'">'+(r.level?prizeMoneyText(lotteryPrizeMoney(id,r.level)):'-')+'</div></td><td>'+lotPickHtml(r.actual,true)+'</td><td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(id)+',win-'+escHtmlAttr(r.p)+'">展开</button></td></tr>');h.push('<tr class="pred-detail" id="'+id+'-detail-win-'+r.p+'"><td colspan="6">'+lotPrizeDetailHtml(id,r)+'</td></tr>')});
  h.push('</tbody></table></div></div>');
  return h.join('');
}
function lotPrizeLoad(id){try{var v=JSON.parse(localStorage.getItem(id+'_lot_prizes')||'[]');return Array.isArray(v)?v:[]}catch(e){return[]}}
function lotPrizeSave(id,v){try{localStorage.setItem(id+'_lot_prizes',JSON.stringify(v))}catch(e){}}
function hitCount(a,b){var c=0;for(var i=0;i<a.length;i++){if(b.indexOf(a[i])>=0)c++}return c}
function sameDigits(a,b){if(a.length!==b.length)return false;for(var i=0;i<a.length;i++){if(a[i]!==b[i])return false}return true}
function sameMulti(a,b){return a.slice().sort().join(',')===b.slice().sort().join(',')}
function judgeLotteryPrize(id,pred,actual){
  var pm=(pred[0]&&pred[0].nums)||[],pe=(pred[1]&&pred[1].nums)||[],am=(actual[0]&&actual[0].nums)||[],ae=(actual[1]&&actual[1].nums)||[],fh=0,bh=0,level=0,hit='';
  if(id==='qlc'){fh=hitCount(pm,am);bh=hitCount(pm.concat(pe),ae);if(fh===7)level=1;else if(fh===6&&bh)level=2;else if(fh===6)level=3;else if(fh===5&&bh)level=4;else if(fh===5||fh===4&&bh)level=5;else if(fh===4||fh===3&&bh)level=6;else if(fh===3||fh===2&&bh)level=7;hit=fh+'基本号+'+bh+'特别号'}
  else if(id==='fc3d'||id==='pl3'){if(sameDigits(pm,am))level=1;else if(sameMulti(pm,am))level=(am[0]===am[1]||am[0]===am[2]||am[1]===am[2])?2:3;fh=sameDigits(pm,am)?3:hitCount(pm,am);hit=fh+'/3'}
  else if(id==='pl5'){fh=0;for(var i=0;i<5;i++){if(pm[i]===am[i])fh++}level=fh===5?1:0;hit=fh+'/5'}
  else if(id==='qxc'){fh=0;for(var q=0;q<7;q++){if(pm[q]===am[q])fh++}if(fh===7)level=1;else if(fh===6)level=2;else if(fh===5)level=3;else if(fh===4)level=4;hit=fh+'/7'}
  else if(id==='kl8'){fh=hitCount(pm,am);if(fh===10)level=1;else if(fh===9)level=2;else if(fh===8)level=3;else if(fh===7)level=4;else if(fh===6)level=5;else if(fh===5)level=6;else if(fh===0)level=7;hit=fh+'/10'}
  return {level:level,hitText:hit||fh+'+'+bh};
}
function saveLotteryPrizeResult(id,p,actual){
  var e=lotFindPending(lotPending(id),p);if(!e)return;
  var best={level:0,bestGroup:1,hitText:'0',actual:actual,groups:[]};
  function score(t){var m=String(t||'').match(/\d+/);return m?parseInt(m[0],10):0}
  e.groups.forEach(function(g,idx){var r=judgeLotteryPrize(id,g.pick,actual),one={idx:idx+1,name:g.name,pick:g.pick,level:r.level,hitText:r.hitText};best.groups.push(one);if((r.level&&(!best.level||r.level<best.level))||(!best.level&&score(r.hitText)>score(best.hitText)))best={level:r.level,bestGroup:idx+1,hitText:r.hitText,actual:actual,groups:best.groups}});
  var arr=lotPrizeLoad(id).filter(function(x){return x.p!==p});
  arr.unshift({p:p,bestGroup:best.bestGroup,level:best.level,hitText:best.hitText,actual:actual,groups:best.groups,date:new Date().toISOString().split('T')[0]});
  while(arr.length>100)arr.pop();
  lotPrizeSave(id,arr);
}
function lotPrizeDetailHtml(id,r){
  var rows=r.groups||[],out=['<div class="pred-detail-box"><div style="color:#ffa500;margin-bottom:6px">五组预测中奖状态</div>'];
  if(!rows.length){out.push('<div style="color:#667788">暂无五组明细，请用新版重新同步或录入本期开奖</div></div>');return out.join('')}
  rows.forEach(function(g){
    out.push('<div class="pred-line"><span class="pred-name">算法'+g.idx+'</span>'+lotPickHtml(g.pick,true)+'<span class="cpred-m">'+(g.name||'')+' · 命中 '+g.hitText+' · '+lotteryPrizeDisplay(id,g.level)+'</span></div>');
  });
  out.push('</div>');
  return out.join('');
}
function lotPrizeTableHtml(id){
  var rows={qlc:[['一等奖','7个基本号',1],['二等奖','6个基本号+特别号',2],['三等奖','6个基本号',3],['四等奖','5个基本号+特别号',4],['五等奖','5个基本号或4个基本号+特别号',5],['六等奖','4个基本号或3个基本号+特别号',6],['七等奖','3个基本号或2个基本号+特别号',7]],fc3d:[['直选','三位号码按位全中',1],['组选3','组三形态号码全中',2],['组选6','组六形态号码全中',3]],pl3:[['直选','三位号码按位全中',1],['组选3','组三形态号码全中',2],['组选6','组六形态号码全中',3]],pl5:[['一等奖','五位号码按位全中',1]],qxc:[['一等奖','7位全中',1],['二等奖','按位中6位',2],['三等奖','按位中5位',3],['四等奖','按位中4位',4],['五等奖','按位中3位',5],['六等奖','按位中2位',6]],kl8:[['选十一等奖','中10',1],['二等奖','中9',2],['三等奖','中8',3],['四等奖','中7',4],['五等奖','中6',5],['六等奖','中5',6],['七等奖','中0',7]]}[id]||[];
  var h=['<div style="margin-top:10px;color:#cfe8ff"><div style="color:#ffa500;margin-bottom:4px">奖级表</div><div class="tw"><table><thead><tr><th>奖级</th><th>中奖条件</th><th>中奖一组预计奖金</th></tr></thead><tbody>'];
  rows.forEach(function(r){h.push('<tr><td>'+r[0]+'</td><td>'+r[1]+'</td><td>'+lotteryPrizeMoney(id,r[2])+'</td></tr>')});
  h.push('</tbody></table></div></div>');
  return h.join('');
}
function buildLotteryPage(id){
  var lot=lotById(id);if(!lot)return;
  var hist=lotHistory(id),np=lotNextPeriod(hist[0].p),entry=lotEnsureEntry(id,np),pred5=lotGetPred5(id),h=[];
  h.push('<div class="header"><h1>'+lot.name+'智能预测</h1><div class="sub">'+lot.type+' · '+lot.desc+'</div></div>');
  h.push('<div class="ccard"><div class="cl">即将开奖</div><div class="cp">第 <span id="'+id+'-npval">'+np+'</span> 期</div>');
  h.push('<div class="ct" id="'+id+'-lot-cd">--天 --时 --分 --秒</div>');
  h.push('<div class="cd" id="'+id+'-lot-cdate">'+(lot.draw||'开奖时间以官方公告为准')+'</div>');
  h.push('<div class="irow" style="margin-top:12px;margin-bottom:6px"><div class="sbox"><div class="sv" style="font-size:16px">'+drawPoolText(hist[0])+'</div><div class="sl">本期奖池</div></div><div class="sbox"><div class="sv" style="font-size:16px">'+drawSalesText(hist[0])+'</div><div class="sl">本期销量</div></div></div>');
  var online=isOnlineMode();
  h.push('<div class="fstat">模式: <span id="'+id+'-fs">'+(isAppMode()?'APP模式':(online?'联网模式':'本地模式'))+'</span><button class="sync-btn" data-action="fetchLotteryAPI" data-args="'+escHtmlAttr(id)+'">同步开奖</button></div>');
  h.push('<div class="cpred"><div class="cpred-t">本期五组预测 <button class="copy-btn" data-action="copyLotteryPred" data-args="'+escHtmlAttr(id)+','+escHtmlAttr(entry.p)+',true">复制五组</button></div>');
  entry.groups.forEach(function(g,idx){
    h.push('<div class="pred-line"><span class="pred-name">算法'+(idx+1)+'</span>'+lotPickHtml(g.pick,false)+'<button class="copy-btn" data-action="copyLotteryPred" data-args="'+escHtmlAttr(id)+','+escHtmlAttr(entry.p)+',false,'+idx+'">复制</button><div class="cpred-m">'+g.name+' · 和值'+lotMetricText(lot,g.pick,'sum')+' · 跨度'+lotMetricText(lot,g.pick,'span')+'</div></div>');
  });
  h.push('</div></div>');
  h.push('<div class="irow">');
  h.push('<div class="sbox"><div class="sv">'+lotPending(id).length+'</div><div class="sl">预测记录</div></div>');
  h.push('<div class="sbox"><div class="sv">'+hist.length+'</div><div class="sl">历史期数</div></div>');
  h.push('<div class="sbox"><div class="sv">--</div><div class="sl">均号码命中</div></div>');
  h.push('<div class="sbox"><div class="sv">--</div><div class="sl">均特别号命中</div></div>');
  h.push('<div class="sbox"><div class="sv">--</div><div class="sl">最佳命中</div></div>');
  h.push('</div>');
  h.push('<div class="pcard"><div class="pcard-h"><span class="pcard-t pulse">未来5期预测</span><span class="pcard-r">第'+pred5[0].p+'期 - 第'+pred5[4].p+'期</span></div>');
  h.push('<div class="tw"><table><thead><tr><th>期号</th><th>号码</th><th>特别号/后区</th><th>和值</th><th>跨度</th><th>奇偶</th><th>区间</th><th>操作</th></tr></thead><tbody>');
  pred5.forEach(function(pr){
    var g0=pr.groups[0].pick,main=g0[0]||{nums:[]},extra=g0[1]||{nums:[]};
    h.push('<tr><td style="color:#ffa500;font-weight:bold">'+pr.p+'</td><td>'+lotPickHtml([main],true)+'</td><td>'+lotPickHtml([extra],true)+'</td><td>'+lotMetricText(lot,g0,'sum')+'</td><td>'+lotMetricText(lot,g0,'span')+'</td><td>'+lotMetricText(lot,g0,'odd')+'</td><td>'+lotMetricText(lot,g0,'zone')+'</td><td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(id)+','+escHtmlAttr(pr.p)+'">展开</button></td></tr>');
    h.push('<tr class="pred-detail" id="'+id+'-detail-'+pr.p+'"><td colspan="8"><div class="pred-detail-box"><div style="text-align:right;margin-bottom:6px"><button class="copy-btn" data-action="copyLotteryPred" data-args="'+escHtmlAttr(id)+','+escHtmlAttr(pr.p)+',true">复制本期五组</button></div>');
    pr.groups.forEach(function(g,idx){h.push('<div class="pred-line"><span class="pred-name">算法'+(idx+1)+'</span>'+lotPickHtml(g.pick,true)+'<span class="cpred-m">'+g.name+'</span></div>')});
    h.push('</div></td></tr>');
  });
  h.push('</tbody></table></div></div>');
  h.push(lotHistoryAccuracyHtml(id,lot));
  h.push('<div class="st">最近10期 <span class="badge">开奖结果</span></div><div class="tw"><table><thead><tr><th>期号</th><th>日期</th><th>号码</th><th>特别号/后区</th><th>销量</th><th>和值</th><th>奇偶</th><th>详情</th></tr></thead><tbody>');
  hist.slice(0,10).forEach(function(r){var main=r.pick[0]||{nums:[]},extra=r.pick[1]||{nums:[]};h.push('<tr><td>'+r.p+'</td><td>'+(r.d||'')+'</td><td>'+lotPickHtml([main],true)+'</td><td>'+lotPickHtml([extra],true)+'</td><td>'+drawSalesText(r)+'</td><td>'+lotMetricText(lot,r.pick,'sum')+'</td><td>'+lotMetricText(lot,r.pick,'odd')+'</td><td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(id)+',draw-'+escHtmlAttr(r.p)+'">查看详情</button></td></tr>');h.push('<tr class="pred-detail" id="'+id+'-detail-draw-'+r.p+'"><td colspan="8">'+prizeRowsHtml(id,r)+'</td></tr>')});
  h.push('</tbody></table></div>');
  h.push('<div class="rand-card"><div class="rand-head"><div class="rand-title">一键随机选号</div><div class="rand-ctrl"><span style="font-size:12px;color:#8899aa">生成组数</span><input type="number" id="'+id+'-lot-count" min="1" max="99" value="5"><button class="btn btn-o" data-action="genLotteryMany" data-args="'+escHtmlAttr(id)+'">随机生成</button><button class="copy-btn" data-action="copyLotteryMany" data-args="'+escHtmlAttr(id)+'">复制</button><button class="copy-btn" data-action="clearLotteryMany" data-args="'+escHtmlAttr(id)+'">清空</button></div></div><div class="rand-out" id="'+id+'-lot-rand"><div style="text-align:center;color:#667788;font-size:12px;padding:8px">设置组数后点击随机生成</div></div></div>');
  h.push('<div class="icard"><h3>手动录入最新开奖</h3><div class="ig" id="'+id+'-manual"><input type="text" id="'+id+'-paste" placeholder="粘贴整组号码，如 4 1 7 7 2 或 6684890" style="min-width:220px"><button class="btn btn-o" data-action="fillLotteryPaste" data-args="'+escHtmlAttr(id)+'">填入</button>'+manualHtml(lot,id)+'<button class="btn btn-o" data-action="submitLotteryR" data-args="'+escHtmlAttr(id)+'">确认</button></div></div>');
  h.push('<div class="st" style="cursor:pointer" data-action="toggleLotteryH" data-args="'+escHtmlAttr(id)+'">完整历史数据 ▾</div><div id="'+id+'-hist" style="display:none"></div>');
  h.push('<div class="rule-card open" id="rule-'+id+'"><div class="rule-head" data-action="toggleRule" data-args="'+escHtmlAttr(id)+'"><span>'+lot.name+'玩法说明</span><span>展开/收起</span></div><div class="rule-body">'+lot.rule+lotPrizeTableHtml(id)+'</div></div>');
  document.getElementById('tab-'+id).innerHTML=h.join('');
  renderLotteryMany(id);
}
function manualHtml(lot,id){
  var h=[];
  if(lot.mode==='balls'){
    lot.sets.forEach(function(s,si){var cnt=(id==='kl8'&&si===0)?20:s.cnt;h.push('<span style="color:#667788;font-size:12px">'+(s.cls==='b'?'后区/蓝球':'号码')+'</span>');for(var i=0;i<cnt;i++)h.push('<input type="number" id="'+id+'-m-'+si+'-'+i+'" min="1" max="'+s.max+'">');if(si<lot.sets.length-1)h.push('<span class="isep">+</span>')});
  }else if(lot.mode==='digits'){
    for(var d=0;d<lot.len;d++)h.push('<input type="number" id="'+id+'-d-'+d+'" min="0" max="9">');
  }else{
    for(var o=0;o<lot.cnt;o++){h.push('<select id="'+id+'-o-'+o+'" style="height:40px;background:#0f1923;color:#fff;border:1px solid #2a4a6a;border-radius:8px">');lot.opts.forEach(function(opt){h.push('<option value="'+opt+'">'+opt+'</option>')});h.push('</select>')}
  }
  return h.join('');
}
function buildLotteryPages(){LOTTERIES.forEach(function(lot){if(lot.id!=='dlt'&&lot.id!=='ssq')buildLotteryPage(lot.id)})}
function lotteryPredText(id,p,all,idx){
  var lot=lotById(id),e=lotFindPending(lotPending(id),p);if(!lot||!e)return '';
  if(all)return lot.name+' '+p+'期\n'+e.groups.map(function(g,i){return '算法'+(i+1)+' '+g.name+': '+lotPickText(lot,g.pick)}).join('\n');
  var g=e.groups[idx||0];return lot.name+' '+p+'期 算法'+((idx||0)+1)+' '+g.name+': '+lotPickText(lot,g.pick);
}
function copyLotteryPred(id,p,all,idx){copyText(lotteryPredText(id,p,all,idx))}
function genLotteryMany(id){var lot=lotById(id),inp=document.getElementById(id+'-lot-count'),n=parseInt(inp&&inp.value)||1;n=Math.max(1,Math.min(99,n));if(inp)inp.value=n;window._lotteryMany[id]=[];for(var i=0;i<n;i++)window._lotteryMany[id].push(lotPick(lot));renderLotteryMany(id)}
function renderLotteryMany(id){var lot=lotById(id),out=document.getElementById(id+'-lot-rand'),arr=window._lotteryMany[id]||[];if(!out)return;if(!arr.length){out.innerHTML='<div style="text-align:center;color:#667788;font-size:12px;padding:8px">暂无随机号码</div>';return}var h=[];arr.forEach(function(pick,idx){h.push('<div class="rand-line"><span class="rand-idx">'+(idx+1)+'组</span>'+lotPickHtml(pick,true)+'</div>')});out.innerHTML=h.join('')}
function copyLotteryMany(id){var lot=lotById(id),arr=window._lotteryMany[id]||[];if(!lot||!arr.length){alert('请先随机生成号码');return}copyText(lot.name+'随机选号\n'+arr.map(function(p,i){return (i+1)+'组 '+lotPickText(lot,p)}).join('\n'))}
function clearLotteryMany(id){window._lotteryMany[id]=[];renderLotteryMany(id)}
function fillLotteryPaste(id){
  var lot=lotById(id),inp=document.getElementById(id+'-paste'),need=0,nums=[];
  if(!lot||!inp)return;
  if(lot.mode==='digits'){
    nums=pasteNums(inp.value,lot.len,true);
    if(nums.length<lot.len){alert('号码数量不够，请检查粘贴内容');return}
    for(var d=0;d<lot.len;d++)document.getElementById(id+'-d-'+d).value=nums[d];
    return;
  }
  if(lot.mode==='balls'){
    lot.sets.forEach(function(s,si){need+=(id==='kl8'&&si===0)?20:s.cnt});
    nums=pasteNums(inp.value,need,false);
    if(nums.length<need){alert('号码数量不够，请检查粘贴内容');return}
    var pos=0;
    lot.sets.forEach(function(s,si){var cnt=(id==='kl8'&&si===0)?20:s.cnt;for(var i=0;i<cnt;i++)document.getElementById(id+'-m-'+si+'-'+i).value=nums[pos++];});
  }
}
function submitLotteryR(id){
  var lot=lotById(id),pick=[],ok=true;
  if(lot.mode==='balls'){
    lot.sets.forEach(function(s,si){var nums=[],cnt=(id==='kl8'&&si===0)?20:s.cnt;for(var i=0;i<cnt;i++){var v=parseInt(document.getElementById(id+'-m-'+si+'-'+i).value);if(!(v>=1&&v<=s.max))ok=false;nums.push(v)}if(hasDup(nums))ok=false;nums.sort(function(a,b){return a-b});pick.push({cls:s.cls,nums:nums})});
  }else if(lot.mode==='digits'){
    var ds=[];for(var d=0;d<lot.len;d++){var v=parseInt(document.getElementById(id+'-d-'+d).value);if(!(v>=0&&v<=9))ok=false;ds.push(v)}pick=[{cls:'d',nums:ds}];
  }else{
    var os=[];for(var o=0;o<lot.cnt;o++)os.push(document.getElementById(id+'-o-'+o).value);pick=[{cls:'o',nums:os}];
  }
  if(!ok){alert('请填写完整且合法的开奖结果');return}
  var hist=lotHistory(id),np=prompt('期号',lotNextPeriod(hist[0].p));if(!np)return;
  hist.unshift({p:np,d:new Date().toISOString().split('T')[0],pick:pick});lotSave(id,'history',hist);saveLotteryPrizeResult(id,np,pick);buildLotteryPage(id);alert(lot.name+' '+np+' 期已录入，已自动判断中奖结果');
}
function toggleLotteryH(id){
  var lot=lotById(id),el=document.getElementById(id+'-hist'),hist=lotHistory(id);
  if(el.style.display==='none'||el.style.display===''){var h=['<div class="tw"><table><thead><tr><th>期号</th><th>日期</th><th>开奖结果</th><th>销量</th><th>详情</th></tr></thead><tbody>'];hist.forEach(function(r){h.push('<tr><td>'+r.p+'</td><td>'+(r.d||'')+'</td><td>'+lotPickHtml(r.pick,true)+'</td><td>'+drawSalesText(r)+'</td><td><button class="expand-btn" data-action="togglePredDetail" data-args="'+escHtmlAttr(id)+',full-'+escHtmlAttr(r.p)+'">查看详情</button></td></tr>');h.push('<tr class="pred-detail" id="'+id+'-detail-full-'+r.p+'"><td colspan="5">'+prizeRowsHtml(id,r)+'</td></tr>')});h.push('</tbody></table></div>');el.innerHTML=h.join('');el.style.display='block'}else el.style.display='none';
}
function handleLotteryLatest(id,d){
  var lot=lotById(id),fs=document.getElementById(id+'-fs'),pick=lotActualFromData(lot,d);
  if(d&&d.period&&pick){d.period=String(d.period);var hist=lotHistory(id),isNew=hist[0].p!==d.period;if(isNew){hist=hist.filter(function(x){return x&&x.p!==d.period});hist.unshift({p:d.period,d:d.date||'',pick:pick,sales:d.sales||'',pool:d.pool||'',grades:d.grades||[]});lotSave(id,'history',hist)}else{hist[0].sales=d.sales||hist[0].sales||'';hist[0].pool=d.pool||hist[0].pool||'';hist[0].grades=d.grades||hist[0].grades||[];if(d.date)hist[0].d=d.date;lotSave(id,'history',hist)}saveLotteryPrizeResult(id,d.period,pick);if(isNew){var pending=lotPending(id).filter(function(x){return x&&x.p&&periodNum(x.p)<periodNum(d.period)});lotSave(id,'pending',pending)}buildLotteryPage(id);fs=document.getElementById(id+'-fs');if(fs){fs.textContent='已同步并判奖';fs.style.color='#4ade80'}}
  else if(fs){fs.textContent='无新数据';fs.style.color='#667788'}
}
window._autoFetchMark=window._autoFetchMark||{};
function autoFetchAfterDraw(id,drawTime){
  var key=id+'_'+(drawTime?drawTime.getTime():new Date().toISOString().split('T')[0]);
  if(window._autoFetchMark[key])return;
  window._autoFetchMark[key]=1;
  setTimeout(function(){fetchLotteryAPI(id,true)},30000);
}
function normalizeRemoteDate(s){
  return String(s||'').replace(/\(.+?\)/g,'').replace(/\//g,'-').trim();
}
function parseNums(s,maxCnt){
  var arr=String(s||'').match(/\d+/g)||[],out=[];
  for(var i=0;i<arr.length&&out.length<maxCnt;i++)out.push(parseInt(arr[i],10));
  return out;
}
function fetchJson(url){
  return fetch(url,{headers:{'Accept':'application/json,text/plain,*/*'}}).then(function(r){return r.text()}).then(function(t){try{return JSON.parse(t)}catch(e){throw '非JSON响应'}});
}
function fromCwlJson(id,j){
  var item=j&&j.result&&j.result[0];if(!item)return null;
  var lot=lotById(id),need=lot.mode==='digits'?lot.len:(id==='kl8'?20:(lot.sets[0].cnt+(lot.sets[1]?lot.sets[1].cnt:0)));
  var nums=parseNums(item.red||item.kjhyjsx,need),back=parseNums(item.blue,5);
  var grades=(item.prizegrades||[]).map(function(g){return {type:g.type,num:g.typenum,money:g.typemoney}});
  var meta={period:item.code,date:normalizeRemoteDate(item.date),sales:item.sales||'',pool:item.poolmoney||'',grades:grades};
  if(id==='qlc'&&nums.length>=8)return Object.assign(meta,{front:nums.slice(0,7),back:nums.slice(7,8)});
  if(id==='fc3d'&&nums.length>=3)return Object.assign(meta,{front:nums.slice(0,3),back:[]});
  if(id==='kl8'&&nums.length>=20)return Object.assign(meta,{front:nums.slice(0,20),back:[]});
  if(nums.length)return Object.assign(meta,{front:nums,back:back});
  return null;
}
function fetchCwlDirect(id){
  var name={qlc:'qlc',fc3d:'3d',kl8:'kl8'}[id];if(!name)return Promise.reject('no cwl');
  var url='https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name='+name+'&issueCount=1';
  return fetchJson(url).then(function(j){var d=fromCwlJson(id,j);if(!d)throw 'empty cwl';return d});
}
function parseEastmoneyHtml(id,html){
  var lot=lotById(id),need=lot.mode==='digits'?lot.len:(lot.sets[0].cnt+(lot.sets[1]?lot.sets[1].cnt:0));
  var re=/<tr[\s\S]*?Result\/Category\/[^?]+\?type=[^&]+&id=(\d+)[\s\S]*?<\/tr>/g,m;
  while((m=re.exec(html))){
    var row=m[0],p=m[1],date=(row.match(/(\d{4}-\d{2}-\d{2})/)||[])[1]||'',plain=row.replace(/<[^>]+>/g,' ').replace(/\s+/g,' ');
    var cells=(row.match(/<td[\s\S]*?<\/td>/g)||[]).map(function(td){return td.replace(/<[^>]+>/g,' ').replace(/\s+/g,' ').trim()});
    var pos=plain.indexOf('详细');if(pos>=0)plain=plain.slice(pos+2);
    var nums=parseNums(plain,need);
    if(nums.length>=need){
      var pool='',sales='',grades=[];
      if(id==='dlt'&&cells.length>=10){
        pool=cells[4]||'';sales=cells[5]||'';
        grades=[{type:'一等奖',num:cells[6]||'',money:cells[7]||''},{type:'一等奖追加',num:cells[8]||'',money:cells[9]||''}];
        return {period:(p.length===5?'20'+p:p),date:date,front:nums.slice(0,5),back:nums.slice(5,7),sales:sales,pool:pool,grades:grades};
      }
      if(id==='ssq'&&cells.length>=10){
        pool=cells[4]||'';sales=cells[5]||'';
        grades=[{type:'一等奖',num:cells[6]||'',money:cells[7]||''},{type:'二等奖',num:cells[8]||'',money:cells[9]||''}];
        return {period:p,date:date,front:nums.slice(0,6),back:nums.slice(6,7),sales:sales,pool:pool,grades:grades};
      }
      if(id==='qxc'&&cells.length>=10){
        pool=cells[4]||'';sales=cells[5]||'';
        grades=[{type:'一等奖',num:cells[6]||'',money:cells[7]||''},{type:'二等奖',num:cells[8]||'',money:cells[9]||''}];
        return {period:p,date:date,front:nums.slice(0,7),back:[],sales:sales,pool:pool,grades:grades};
      }
      if(cells.length>=5){sales=cells[4]||'';}
      if(id==='qlc')return {period:p,date:date,front:nums.slice(0,7),back:nums.slice(7,8),sales:sales,pool:pool,grades:grades};
      return {period:p,date:date,front:nums.slice(0,need),back:[],sales:sales,pool:pool,grades:grades};
    }
  }
  return null;
}
function fetchEastmoneyDirect(id){
  var url='https://caipiao.eastmoney.com/pub/Result/History/'+id+'/';
  return fetch(url).then(function(r){return r.text()}).then(function(html){var d=parseEastmoneyHtml(id,html);if(!d)throw 'empty eastmoney';return d});
}
function parseShSportsDltHtml(html){
  var text=String(html||'').replace(/<[^>]+>/g,' ').replace(/&nbsp;/g,' ').replace(/\s+/g,' ');
  var p=(text.match(/第\s*(\d{5})\s*期开奖公告/)||text.match(/第\s*(\d{5})\s*期/)||[])[1];
  var date=(text.match(/开奖日期[:：]\s*(\d{4})年(\d{2})月(\d{2})日/)||[]);
  var d=date.length?date[1]+'-'+date[2]+'-'+date[3]:'';
  var sales=(text.match(/本期全国销售金额[:：]\s*([\d,]+元)/)||[])[1]||'';
  var pool=(text.match(/([\d,]+\.\d+元)奖金滚入下期奖池/)||text.match(/([\d,]+元)奖金滚入下期奖池/)||[])[1]||'';
  var numsPart=(text.match(/本期开奖号码[:：]\s*([0-9\s]+?)\s*(?:本期中奖情况|奖级|$)/)||[])[1]||'';
  var nums=parseNums(numsPart,7);
  if(!p||nums.length<7)return null;
  var grades=[];
  var g1=(text.match(/一等奖\s+基本\s+(\d+注)\s+([\d,]+元)/)||[]);
  var g1a=(text.match(/追加\s+(\d+注)\s+([\d,]+元)/)||[]);
  if(g1.length)grades.push({type:'一等奖',num:g1[1],money:g1[2]});
  if(g1a.length)grades.push({type:'一等奖追加',num:g1a[1],money:g1a[2]});
  return {period:'20'+p.slice(0,2)+p.slice(2),date:d,front:nums.slice(0,5),back:nums.slice(5,7),sales:sales,pool:pool,grades:grades};
}
function fetchShSportsDltDirect(){
  return fetch('https://www.shsportslottery.com/dlt/kjgg.html').then(function(r){return r.text()}).then(function(html){var d=parseShSportsDltHtml(html);if(!d)throw 'empty shsports dlt';return d});
}
function parseVipcKl8Html(html,period){
  var p=(html.match(/快乐8(\d+)期开奖结果/)||html.match(/第\s*(\d+)\s*期/)||[])[1]||period;
  var date=((html.match(/开奖时间：\s*(\d{4}-\d{2}-\d{2})/)||[])[1])||'';
  var part=html.split('开奖时间')[0],nums=parseNums(part,20);
  if(nums.length<20)return null;
  var sales=(html.match(/本期销量：\s*([\d,\.]+\s*元)/)||[])[1]||'';
  var pool=(html.match(/奖池滚存：\s*([\d,\.]+\s*元)/)||[])[1]||'';
  var grades=[],play='';
  html.split('\n').forEach(function(line){
    var cells=line.split('|').map(function(x){return x.trim()}).filter(Boolean);
    if(cells.length>=4&&/^选/.test(cells[0])){
      play=cells[0];grades.push({type:play+cells[1],num:cells[2],money:cells[3]});
    }else if(cells.length>=3&&/^中/.test(cells[0])&&play){
      grades.push({type:play+cells[0],num:cells[1],money:cells[2]});
    }
  });
  return {period:p,date:date,front:nums.slice(0,20),back:[],sales:sales,pool:pool,grades:grades};
}
function fetchVipcKl8Direct(){
  var h=lotHistory('kl8'),p=(h&&h[0]&&h[0].p)||'2026170';
  return fetch('https://www.vipc.cn/result/kl8/'+p).then(function(r){return r.text()}).then(function(html){var d=parseVipcKl8Html(html,p);if(!d)throw 'empty vipc kl8';return d});
}
function fetchDirectLatest(id){
  if(id==='kl8')return fetchCwlDirect(id).catch(function(){return fetchVipcKl8Direct()});
  if(id==='qlc'||id==='fc3d')return fetchCwlDirect(id).catch(function(){return fetchEastmoneyDirect(id)});
  if(id==='dlt')return fetchShSportsDltDirect().catch(function(){return fetchEastmoneyDirect(id)});
  return fetchEastmoneyDirect(id);
}
function fetchLotteryAPI(id){
  var fs=document.getElementById(id+'-fs');
  if(isAppMode()){if(fs){fs.textContent='同步中...';fs.style.color='#fbbf24'}try{var raw=AndroidSync.fetchLatest(id),data=JSON.parse(raw||'[]'),d0=Array.isArray(data)?data[0]:data;if(!d0||!d0.period)throw 'empty app data';handleLotteryLatest(id,d0)}catch(e){fetchDirectLatest(id).then(function(d){handleLotteryLatest(id,d)}).catch(function(){fs=document.getElementById(id+'-fs');if(fs){fs.textContent='同步失败，可手动录入';fs.style.color='#f87171'}})}return}
  if(!canUseLocalApi()){
    if(fs){fs.textContent='联网同步中...';fs.style.color='#fbbf24'}
    fetchDirectLatest(id).then(function(d){handleLotteryLatest(id,d)}).catch(function(e){
      buildLotteryPage(id);fs=document.getElementById(id+'-fs');
      if(fs){fs.textContent='无法联网采集，可手动录入';fs.style.color='#f87171'}
    });
    return
  }
  if(fs){fs.textContent='同步中...';fs.style.color='#fbbf24'}
  fetch('/api/'+id+'/latest').then(function(r){return r.json()}).then(function(data){handleLotteryLatest(id,Array.isArray(data)?data[0]:data)}).catch(function(e){
    fetchDirectLatest(id).then(function(d){handleLotteryLatest(id,d)}).catch(function(){
      fs=document.getElementById(id+'-fs');if(fs){fs.textContent='同步失败，可手动录入';fs.style.color='#f87171'}
    });
  });
}
function toggleRule(id){var el=document.getElementById('rule-'+id);if(el)el.classList.toggle('open')}

// ===== 彩票页面初始化 =====
window.initLotteryPage = function() {
  var container = document.getElementById('lotteryPageContainer');
  if (!container) return;
  
  // 初始化各彩种页面
  buildLotteryPage('dlt');
  buildLotteryPage('ssq');
  buildLotteryPage('qlc');
  buildLotteryPage('fc3d');
  buildLotteryPage('pl3');
  buildLotteryPage('pl5');
  buildLotteryPage('qxc');
  buildLotteryPage('kl8');
  
  // 初始化标签切换
  initTabSnap();
  
  // 加载历史数据
  loadLotteryHistory();
};

// 加载彩票历史数据
function loadLotteryHistory() {
  var lotteries = ['dlt', 'ssq', 'qlc', 'fc3d', 'pl3', 'pl5', 'qxc', 'kl8'];
  lotteries.forEach(function(id) {
    var histKey = 'lottery_history_' + id;
    try {
      var hist = JSON.parse(localStorage.getItem(histKey) || '[]');
      if (hist.length > 0) {
        // 已有本地数据，不需要重新加载
      }
    } catch(e) {}
  });
}

// ===== 三角洲计算器 =====
function deltaCalc() {
  var pureCoin = parseFloat(document.getElementById('dpureCoin').value) || 0;
  var ratio = parseInt(document.getElementById('dratio').value) || 40;
  var awm = parseInt(document.getElementById('dawm').value) || 0;
  var awmP = parseFloat(document.getElementById('dawmP').value) || 0;
  var red = parseInt(document.getElementById('dred').value) || 0;
  var redP = parseFloat(document.getElementById('dredP').value) || 0;
  var helm = parseInt(document.getElementById('dhelm').value) || 0;
  var helmP = parseFloat(document.getElementById('dhelmP').value) || 0;
  var armor = parseInt(document.getElementById('darmor').value) || 0;
  var armorP = parseFloat(document.getElementById('darmorP').value) || 0;
  
  var pureVal = (pureCoin * 1000000 / ratio / 10000).toFixed(2);
  var awmVal = (awm * awmP).toFixed(2);
  var redVal = (red * redP).toFixed(2);
  var helmVal = (helm * helmP).toFixed(2);
  var armorVal = (armor * armorP).toFixed(2);
  var total = (parseFloat(pureVal) + parseFloat(awmVal) + parseFloat(redVal) + parseFloat(helmVal) + parseFloat(armorVal)).toFixed(2);
  
  document.getElementById('drPure').textContent = pureVal + ' 元';
  document.getElementById('drAWM').textContent = awmVal + ' 元';
  document.getElementById('drRed').textContent = redVal + ' 元';
  document.getElementById('drHelm').textContent = helmVal + ' 元';
  document.getElementById('drArmor').textContent = armorVal + ' 元';
  document.getElementById('drTotal').textContent = total + ' 元';
  document.getElementById('drDetail').textContent = '纯币:' + pureCoin + 'M(' + ratio + ':1) AWM:' + awm + '(' + awmP + '/发) 红弹:' + red + '(' + redP + '/发) 头盔:' + helm + '(' + helmP + '/个) 护甲:' + armor + '(' + armorP + '/个)';
}

function deltaReset() {
  document.getElementById('dpureCoin').value = '';
  document.getElementById('dratio').value = '40';
  document.getElementById('dawm').value = '';
  document.getElementById('dawmP').value = '';
  document.getElementById('dred').value = '';
  document.getElementById('dredP').value = '';
  document.getElementById('dhelm').value = '';
  document.getElementById('dhelmP').value = '';
  document.getElementById('darmor').value = '';
  document.getElementById('darmorP').value = '';
  document.getElementById('drPure').textContent = '0.00 元';
  document.getElementById('drAWM').textContent = '0.00 元';
  document.getElementById('drRed').textContent = '0.00 元';
  document.getElementById('drHelm').textContent = '0.00 元';
  document.getElementById('drArmor').textContent = '0.00 元';
  document.getElementById('drTotal').textContent = '0.00 元';
  document.getElementById('drDetail').textContent = '输入数据后点击"计算总价"';
}

function deltaDemo() {
  document.getElementById('dpureCoin').value = '100';
  document.getElementById('dratio').value = '40';
  document.getElementById('dawm').value = '10';
  document.getElementById('dawmP').value = '0.8';
  document.getElementById('dred').value = '100';
  document.getElementById('dredP').value = '0.15';
  document.getElementById('dhelm').value = '5';
  document.getElementById('dhelmP').value = '1.5';
  document.getElementById('darmor').value = '5';
  document.getElementById('darmorP').value = '1.5';
  deltaCalc();
}

// ===== 彩票模块全局事件委托（替代 innerHTML onclick） =====
(function() {
  var container = document.getElementById('lotteryPageContainer');
  if (!container) return;
  container.addEventListener('click', function(e) {
    var target = e.target;
    var btn = target.tagName === 'BUTTON' ? target : target.closest('button');
    if (!btn) return;
    var text = (btn.textContent || '').trim();
    var cls = btn.className || '';
    var row = btn.closest('tr');
    var id = null;

    if (row) id = row.id;
    if (!id) {
      var panel = btn.closest('[id]');
      if (panel) id = panel.id;
    }

    if (cls.indexOf('sync-btn') >= 0) {
      var lotId = id ? id.replace('-fs','') : '';
      if (window.fetchAPI) return window.fetchAPI(lotId);
      if (window.fetchLotteryAPI) return window.fetchLotteryAPI(lotId);
    }
    if (cls.indexOf('copy-btn') >= 0 || text === '复制' || text === '复制五组' || text === '复制本期五组') {
      if (window.copyPred) return window.copyPred(id, id, true);
      if (window.copyLotteryPred) return window.copyLotteryPred(id, id, true);
    }
    if (cls.indexOf('expand-btn') >= 0 || text === '展开' || text === '查看详情') {
      var period = (id || '').replace('detail-', '').replace('win-', '').replace('draw-', '').replace('full-', '');
      if (window.togglePredDetail) return window.togglePredDetail(id, period);
    }
    if (text === '随机生成' || cls.indexOf('btn-o') >= 0) {
      if (id && text === '随机生成' && window.genRandomNums) return window.genRandomNums(id);
      if (id && text === '随机生成' && window.genLotteryMany) return window.genLotteryMany(id);
    }
    if (text === '复制' && cls.indexOf('copy-btn') >= 0) {
      if (id && window.copyRandomNums) return window.copyRandomNums(id);
      if (id && window.copyLotteryMany) return window.copyLotteryMany(id);
    }
    if (text === '清空') {
      if (id && window.clearRandomNums) return window.clearRandomNums(id);
      if (id && window.clearLotteryMany) return window.clearLotteryMany(id);
    }
    if (text === '确认') {
      if (id && window.submitR) return window.submitR(id);
      if (id && window.submitLotteryR) return window.submitLotteryR(id);
    }
    if (text === '填入') {
      if (id && window.fillMainPaste) return window.fillMainPaste(id);
      if (id && window.fillLotteryPaste) return window.fillLotteryPaste(id);
    }
    if (text === '完整历史数据 ▾' || target.classList.contains('st')) {
      if (id && window.toggleH) return window.toggleH(id);
      if (id && window.toggleLotteryH) return window.toggleLotteryH(id);
    }
    if (target.classList.contains('rule-head') || text.indexOf('玩法说明') >= 0) {
      if (id && window.toggleRule) return window.toggleRule(id);
    }
  });
})();

