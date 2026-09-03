<template>
  <div>
    <div class="page-intro">
      <div>
        <span class="section-kicker">经过审核的内容</span>
        <h2>知识库</h2>
        <p>从一篇短文章或一个轻量练习开始，不需要一次解决所有问题。</p>
      </div>
      <label class="search-box">
        <span>⌕</span>
        <input v-model="keyword" type="search" placeholder="搜索文章" @input="debouncedSearch" />
      </label>
    </div>

    <div class="category-tabs">
      <button v-for="tab in categoryTabs" :key="tab" type="button"
              :class="{ active: selectedCategory === tab }" @click="selectCategory(tab)">{{ tab }}</button>
    </div>

    <div class="library-layout">
      <section class="panel article-list-panel">
        <div class="section-heading">
          <h2>文章与练习</h2>
          <span class="muted" style="color:var(--muted);font-size:12px">{{ articles.length }} 篇已发布</span>
        </div>
        <div v-if="articles.length" class="article-list">
          <button v-for="article in articles" :key="article.id" type="button"
                  class="article-card" :class="{ active: selectedArticle?.id === article.id }"
                  @click="selectArticle(article)">
            <span class="article-card-thumb">{{ article.categoryName || '知识' }}</span>
            <span>
              <strong>{{ article.title }}</strong>
              <small>{{ article.categoryName }} · {{ article.minutes || 5 }} 分钟阅读 · 已审核</small>
            </span>
            <span class="status-chip success">已发布</span>
          </button>
        </div>
        <div v-else class="empty-state">没有找到符合条件的已发布内容。</div>
      </section>

      <article class="panel article-detail">
        <template v-if="selectedArticle">
          <span class="tag focus">已审核</span>
          <h2>{{ selectedArticle.title }}</h2>
          <p v-for="(paragraph, index) in renderContent(selectedArticle.content)" :key="index">{{ paragraph }}</p>
          <div class="article-divider"></div>
          <div class="article-tip">{{ selectedArticle.summary || '本文档由运营团队审核后发布。' }}</div>
        </template>
        <div v-else class="empty-state">选择一篇文章开始阅读。</div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listKnowledge } from '../api'

const keyword = ref('')
const selectedCategory = ref('全部')
const categoryTabs = ['全部', '压力', '睡眠', '关系', '自助练习']
const articles = ref([])
const selectedArticle = ref(null)
let searchTimer = null

async function load() {
  try {
    const result = await listKnowledge({
      keyword: keyword.value || undefined,
      category: selectedCategory.value === '全部' ? undefined : selectedCategory.value,
      page: 1,
      pageSize: 50
    })
    articles.value = result?.records || []
    if (!selectedArticle.value || !articles.value.some((a) => a.id === selectedArticle.value.id)) {
      selectedArticle.value = articles.value[0] || null
    }
  } catch (e) {
    articles.value = []
  }
}

function selectCategory(tab) {
  selectedCategory.value = tab
  load()
}

function selectArticle(article) {
  selectedArticle.value = article
}

function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(load, 350)
}

function renderContent(content) {
  if (!content) return []
  const text = String(content)
    .replace(/<p[^>]*>/g, '')
    .replace(/<\/p>/g, '\n')
    .replace(/<br\s*\/?>/g, '\n')
    .replace(/<[^>]+>/g, '')
  return text.split('\n').map((line) => line.trim()).filter(Boolean)
}

onMounted(load)
</script>
