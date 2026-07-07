hexo.extend.helper.register('wordcount', function(content = '') {
  const text = String(content).replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
  if (!text) return 0;
  const cjk = (text.match(/[\u4e00-\u9fff]/g) || []).length;
  const words = (text.replace(/[\u4e00-\u9fff]/g, ' ').match(/[A-Za-z0-9_]+/g) || []).length;
  return cjk + words;
});

hexo.extend.helper.register('min2read', function(content = '', options = {}) {
  const words = this.wordcount(content);
  const speed = options.en || 200;
  return Math.max(1, Math.ceil(words / speed));
});
