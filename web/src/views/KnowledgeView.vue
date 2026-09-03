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
      <span class="category-tabs-sep"></span>
      <button type="button" class="tab-favorite" :class="{ active: onlyFavorites }"
              @click="toggleFavoritesView">♡ {{ onlyFavorites ? '我的收藏' : '收藏夹' }}</button>
    </div>

    <div v-if="!onlyFavorites && tags.length" class="tag-chips">
      <span class="muted" style="color:var(--muted);font-size:12px">标签</span>
      <button v-for="tag in tags" :key="tag" type="button" class="tag-chip"
              :class="{ active: selectedTag === tag }" @click="toggleTag(tag)"># {{ tag }}</button>
    </div>

    <div class="library-layout">
      <section class="panel article-list-panel">
        <div class="section-heading">
          <h2>{{ onlyFavorites ? '我的收藏' : '文章与练习' }}</h2>
          <span class="muted" style="color:var(--muted);font-size:12px">{{ articles.length }} 篇{{ onlyFavorites ? '已收藏' : '已发布' }}</span>
        </div>
        <div v-if="articles.length" class="article-list">
          <button v-for="article in articles" :key="article.id" type="button"
                  class="article-card" :class="{ active: selectedArticle?.id === article.id }"
                  @click="selectArticle(article)">
            <span class="article-card-thumb">{{ article.categoryName || '知识' }}</span>
            <span>
              <strong>{{ article.title }}</strong>
              <small>{{ article.categoryName }} · {{ article.minutes || 5 }} 分钟阅读 · 已审核</small>
              <small v-if="article.tags" class="article-tags">{{ article.tags.split(',').map((t) => '#' + t.trim()).join(' ') }}</small>
            </span>
            <span class="favorite-star" :class="{ on: favoriteIds.has(article.id) }"
                  :title="favoriteIds.has(article.id) ? '取消收藏' : '收藏'"
                  @click.stop="toggleFavorite(article)">
              {{ favoriteIds.has(article.id) ? '★' : '☆' }}
            </span>
          </button>
        </div>
        <div v-else class="empty-state">{{ onlyFavorites ? '还没有收藏文章，点文章卡片右侧的星标即可收藏。' : '没有找到符合条件的已发布内容。' }}</div>
      </section>

      <article class="panel article-detail">
        <template v-if="selectedArticle">
          <div class="detail-head">
            <span class="tag focus">已审核</span>
            <span class="favorite-star detail" :class="{ on: favoriteIds.has(selectedArticle.id) }"
                  @click="toggleFavorite(selectedArticle)">
              {{ favoriteIds.has(selectedArticle.id) ? '★ 已收藏' : '☆ 收藏' }}
            </span>
          </div>
          <h2>{{ selectedArticle.title }}</h2>
          <p v-for="(paragraph, index) in renderContent(selectedArticle.content)" :key="index">{{ paragraph }}</p>
          <div v-if="selectedArticle.tags" class="article-tagline">
            <span v-for="tag in selectedArticle.tags.split(',')" :key="tag" class="tag-chip static"># {{ tag.trim() }}</span>
          </div>
          <div class="article-divider"></div>
          <div class="article-tip">{{ selectedArticle.summary || '本文档由运营团队审核后发布。' }}</div>
        </template>
        <div v-else class="empty-state">选择一篇文章开始阅读。</div>
      </article>
    </div>

    <section v-if="!onlyFavorites && recommend.length" class="panel recommend-panel">
      <div class="section-heading">
        <h2>为你推荐</h2>
        <span class="muted" style="color:var(--muted);font-size:12px">根据你的收藏偏好挑选的已审核文章</span>
      </div>
      <div class="recommend-list">
        <button v-for="article in recommend" :key="article.id" type="button"
                class="recommend-card" @click="selectArticle(article)">
          <strong>{{ article.title }}</strong>
          <small>{{ article.categoryName }} · {{ article.minutes || 5 }} 分钟阅读 · 已审核</small>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listKnowledge, listKnowledgeTags, listRecommendArticles, pageMyFavorites, myFavoriteIds, addFavorite, removeFavorite } from '../api'

const keyword = ref('')
const selectedCategory = ref('全部')
const selectedTag = ref('')
const onlyFavorites = ref(false)
const categoryTabs = ['全部', '压力', '睡眠', '关系', '自助练习']
const tags = ref([])
const articles = ref([])
const selectedArticle = ref(null)
const recommend = ref([])
const favoriteIds = ref(new Set())
let searchTimer = null

async function loadTags() {
  try {
    tags.value = await listKnowledgeTags()
  } catch (e) {
    tags.value = []
  }
}

async function loadFavorites() {
  try {
    const ids = await myFavoriteIds()
    favoriteIds.value = new Set(ids || [])
  } catch (e) {
    favoriteIds.value = new Set()
  }
}

async function load() {
  try {
    let records = []
    if (onlyFavorites.value) {
      const result = await pageMyFavorites({ page: 1, pageSize: 50 })
      records = result?.records || []
    } else {
      const result = await listKnowledge({
        keyword: keyword.value || undefined,
        category: selectedCategory.value === '全部' ? undefined : selectedCategory.value,
        tag: selectedTag.value || undefined,
        page: 1,
        pageSize: 50
      })
      records = result?.records || []
    }
    articles.value = records
    if (!selectedArticle.value || !articles.value.some((a) => a.id === selectedArticle.value.id)) {
      selectedArticle.value = articles.value[0] || null
    }
  } catch (e) {
    articles.value = []
  }
}

async function loadRecommend() {
  try {
    recommend.value = await listRecommendArticles(6)
  } catch (e) {
    recommend.value = []
  }
}

function selectCategory(tab) {
  selectedCategory.value = tab
  if (onlyFavorites.value) onlyFavorites.value = false
  load()
}

function toggleTag(tag) {
  selectedTag.value = selectedTag.value === tag ? '' : tag
  if (onlyFavorites.value) onlyFavorites.value = false
  load()
}

function toggleFavoritesView() {
  onlyFavorites.value = !onlyFavorites.value
  if (onlyFavorites.value) {
    selectedTag.value = ''
    keyword.value = ''
  }
  load()
}

async function toggleFavorite(article) {
  try {
    if (favoriteIds.value.has(article.id)) {
      await removeFavorite(article.id)
      favoriteIds.value.delete(article.id)
    } else {
      await addFavorite(article.id)
      favoriteIds.value.add(article.id)
    }
    favoriteIds.value = new Set(favoriteIds.value)
    if (onlyFavorites.value) load()
  } catch (e) {
    // 登录后接口失败时不刷新视图
  }
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

onMounted(() => {
  load()
  loadTags()
  loadFavorites()
  loadRecommend()
})
</script>

<style scoped>
.category-tabs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.category-tabs-sep {
  width: 1px;
  height: 18px;
  background: var(--border, #e5e7eb);
  margin: 0 4px;
}
.tab-favorite {
  border: 1px solid var(--border, #e5e7eb);
  background: #fff;
  color: var(--text, #111);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
}
.tab-favorite.active {
  background: #fff3e0;
  border-color: #f59e0b;
  color: #b45309;
}
.tag-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.tag-chip {
  border: 1px solid var(--border, #e5e7eb);
  background: #fff;
  color: var(--muted, #6b7280);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
}
.tag-chip.active {
  background: #eef2ff;
  border-color: #6366f1;
  color: #4f46e5;
}
.tag-chip.static {
  cursor: default;
  background: #f9fafb;
}
.article-card {
  position: relative;
}
.favorite-star {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  border-radius: 8px;
  color: #9ca3af;
  font-size: 16px;
  cursor: pointer;
  user-select: none;
}
.favorite-star.on {
  color: #f59e0b;
}
.favorite-star.detail {
  border: 1px solid var(--border, #e5e7eb);
  padding: 0 10px;
  font-size: 13px;
  border-radius: 999px;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.article-tags {
  display: block;
  color: var(--muted, #6b7280);
  margin-top: 2px;
  font-size: 11px;
}
.article-tagline {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 10px 0;
}
.recommend-panel {
  margin-top: 20px;
}
.recommend-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.recommend-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
  padding: 14px;
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s;
}
.recommend-card:hover {
  border-color: #6366f1;
}
.recommend-card small {
  color: var(--muted, #6b7280);
}
</style>
