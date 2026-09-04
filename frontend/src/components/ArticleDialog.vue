<template>
    <el-dialog
        :title="isEdit ? '编辑文章' : '新增文章'"
        v-model="dialogVisible"
        width="50%"
        @close="handleClose"
    >
        <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
            <el-form-item label="文档导入">
                <div
                    class="doc-import"
                    :class="{ 'is-dragover': dragOver }"
                    @click="fileInputRef?.click()"
                    @dragover.prevent="dragOver = true"
                    @dragleave.prevent="dragOver = false"
                    @drop.prevent="handleDrop"
                >
                    <input
                        ref="fileInputRef"
                        type="file"
                        class="doc-input"
                        accept=".txt,.md,.markdown,.pdf,.doc,.docx"
                        @change="handleFileChange"
                    />
                    <template v-if="importing">
                        <el-icon class="doc-icon is-loading"><Loading /></el-icon>
                        <p class="doc-tip">正在解析「{{ importingName }}」…</p>
                        <p class="doc-sub">请稍候，正在识取文档内容</p>
                    </template>
                    <template v-else-if="importedFile">
                        <el-icon class="doc-icon success"><CircleCheckFilled /></el-icon>
                        <p class="doc-tip">{{ importedFile.name }}<span class="doc-size">（{{ formatSize(importedFile.size) }}）</span></p>
                        <p class="doc-sub">已识别标题与正文，可修改后保存；点击此处重新选择</p>
                    </template>
                    <template v-else>
                        <el-icon class="doc-icon"><Document /></el-icon>
                        <p class="doc-tip">将文档拖拽到此处，或 <span class="doc-link">点击选择文件</span></p>
                        <p class="doc-sub">支持 txt / md / pdf / doc / docx，自动识别文档标题与正文</p>
                    </template>
                </div>
            </el-form-item>
            <el-form-item label="文章标题" prop="title">
                <el-input v-model="formData.title" placeholder="请输入文章标题" maxlength="200" show-word-limit clearable />
            </el-form-item>
            <el-form-item label="所属分类" prop="categoryId">
                <el-select v-model="formData.categoryId" placeholder="请选择分类">
                    <el-option v-for="item in props.categories" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="文章摘要" prop="summary">
                <el-input type="textarea" v-model="formData.summary" placeholder="请输入文章摘要(可选)" maxlength="1000" show-word-limit :rows="4" />
            </el-form-item>
            <el-form-item label="标签" prop="tags">
                <el-select v-model="formData.tagArray" placeholder="请输入文章标签(逗号分隔)" multiple filterable allow-create style="width: 100%">
                    <el-option v-for="tag in commonTags" :key="tag" :label="tag" :value="tag" />
                </el-select>
            </el-form-item>
            <el-form-item label="封面图片">
                <div class="cover-upload">
                    <el-upload
                        class="avatar-uploader"
                        action="#"
                        :before-upload="beforeUpload"
                        :http-request="handleUploadRequest"
                        :show-file-list="false"
                        accept="image/*"
                    >
                        <div v-if="!imgUrl" class="cover-placeholder">
                            <p>点击上传封面</p>
                        </div>
                        <img v-else :src="imgUrl" class="cover-image" alt="封面图片" />
                    </el-upload>
                    <div v-if="imgUrl" class="cover-remove">
                        <el-button type="danger" size="mini" @click="handleRemove">移除封面</el-button>
                    </div>
                </div>
            </el-form-item>
            <el-form-item label="文章内容" prop="content">
                <RichTextEditor
                    v-model="formData.content"
                    placeholder="请输入文章内容，支持富文本格式\n\n可以使用加粗、斜体、列表、标题等格式来丰富文章内容。"
                    :maxCharCount="100000"
                    @change="handleContentChange"
                    @created="handleEditorCreated"
                    min-height="400px"
                    />
            </el-form-item>
        </el-form>
        <div v-if="btnPreview">
            <h3>内容预览</h3>
            <div v-html="formData.content"></div>
        </div>
        <template #footer>
            <el-button @click="btnPreview = !btnPreview">{{ btnPreview ? '隐藏预览' : '预览效果' }}</el-button>
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="loading">{{ isEdit ? '更新文章' : '创建文章' }}</el-button>
        </template>
    </el-dialog>
</template>
<script setup>
import { ref, reactive, computed, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFile, createArticle, updateArticle, importDocument } from '@/api/admin'
import { fileBaseUrl } from '@/config/index.js'
import RichTextEditor from '@/components/RichTextEditor.vue'

const props = defineProps({
    modelValue: {
        type: Boolean,
        default: false
    },
    categories: {
        type: Array,
        default: () => []
    },
    article: {
        type: Object,
        default: null
    }
})

const emit = defineEmits(['update:modelValue', 'success'])

const dialogVisible = computed({
    get() {
        return props.modelValue
    },
    set(val) {
        emit('update:modelValue', val)
    }
})

const isEdit = computed(() => !!props.article?.id)

// 监听编辑数据
watch(() => props.article, (newVal) => {
    if (newVal) {
        nextTick(() => {
            Object.assign(formData, newVal)
            // 使用现有ID
            businessId.value = newVal.id
            // 标签回显：优先 tagArray，否则把 tags 字符串拆分为数组
            if (Array.isArray(newVal.tagArray)) {
                formData.tagArray = [...newVal.tagArray]
            } else if (typeof newVal.tags === 'string' && newVal.tags) {
                formData.tagArray = newVal.tags.split(',').map(t => t.trim()).filter(Boolean)
            } else {
                formData.tagArray = []
            }
            // 封面Url
            imgUrl.value = newVal.coverImage ? fileBaseUrl + newVal.coverImage : ''
        })
    }
})

const handleClose = () => {
    // 重置表单
    formRef.value?.resetFields()
    // 重置ID
    businessId.value = null
    // 重置标签
    formData.tagArray = []
    // 清空富文本编辑器内容，避免下次打开残留
    editorInstance.value?.clear()
    // 重置封面图片和数据
    handleRemove()
    // 重置文档导入状态
    importedFile.value = null
    importing.value = false
    dragOver.value = false
    emit('update:modelValue', false)
}

// 表单数据
const formData = reactive({
    "title": "",
    "content": "",
    "coverImage": "",
    "categoryId": "",
    "summary": "",
    "tags": "",
    "tagArray": [],
    "id": ""
})

const rules = reactive({
    title: [
        { required: true, message: '请输入文章标题', trigger: 'blur' },
        { max: 200, message: '文章标题最多200个字符', trigger: 'blur' }
    ],
    categoryId: [
        { required: true, message: '请选择分类', trigger: 'change' }
    ],
    content: [
        { required: true, message: '请输入文章内容', trigger: 'blur' },
        { max: 100000, message: '文章内容最多100000个字符', trigger: 'blur' }
    ],
})

const commonTags = [
  '情绪管理', '焦虑', '抑郁', '压力', '睡眠', 
  '冥想', '正念', '放松', '心理健康', '自我成长',
  '人际关系', '工作压力', '学习方法', '生活技巧'
]

// 文档导入（拖拽 / 选择上传，后端识取文档内容）
const SUPPORTED_DOC_EXT = ['txt', 'md', 'markdown', 'pdf', 'doc', 'docx']
const fileInputRef = ref(null)
const dragOver = ref(false)
const importing = ref(false)
const importingName = ref('')
const importedFile = ref(null)

const handleDrop = (e) => {
    dragOver.value = false
    const files = e.dataTransfer?.files
    if (files && files.length) {
        handleImport(files[0])
    }
}

const handleFileChange = (e) => {
    const file = e.target.files?.[0]
    if (file) {
        handleImport(file)
    }
    // 清空 input 值，保证可重复选择同一文件
    e.target.value = ''
}

const handleImport = (file) => {
    const ext = (file.name.split('.').pop() || '').toLowerCase()
    if (!SUPPORTED_DOC_EXT.includes(ext)) {
        ElMessage.error('仅支持 txt / md / pdf / doc / docx 文档')
        return
    }
    if (file.size > 10 * 1024 * 1024) {
        ElMessage.error('文档大小不能超过 10MB')
        return
    }
    const hasContent = formData.content && formData.content.trim() && formData.content !== '<p><br></p>'
    if (hasContent) {
        ElMessageBox.confirm('导入文档将覆盖当前文章内容，是否继续？', '确认导入', {
            confirmButtonText: '覆盖并导入',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => doImport(file)).catch(() => {})
    } else {
        doImport(file)
    }
}

const doImport = async (file) => {
    importing.value = true
    importingName.value = file.name
    try {
        const res = await importDocument(file)
        importedFile.value = file
        if (!formData.title) {
            formData.title = res.title
        }
        const html = textToHtml(res.content)
        formData.content = html
        editorInstance.value?.setHtml(html)
        ElMessage.success('文档解析成功，标题与正文已填充')
    } catch (e) {
        // 错误提示已由请求拦截器统一处理
    } finally {
        importing.value = false
    }
}

const formatSize = (bytes) => {
    if (!bytes) return '0 B'
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

// 纯文本 / 轻量 Markdown -> 富文本 HTML
const textToHtml = (text) => {
    const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
    const lines = String(text || '').replace(/\r\n/g, '\n').split('\n')
    const blocks = []
    let para = []
    const flush = () => {
        if (para.length) {
            blocks.push({ type: 'p', text: para.join('\n') })
            para = []
        }
    }
    for (const line of lines) {
        const heading = line.match(/^(#{1,3})\s+(.*)$/)
        if (heading) {
            flush()
            blocks.push({ type: 'h' + heading[1].length, text: heading[2].trim() })
            continue
        }
        if (line.trim() === '') {
            flush()
            continue
        }
        para.push(line)
    }
    flush()
    return blocks.map(b => {
        const content = esc(b.text).replace(/\n/g, '<br>')
        return b.type.startsWith('h') ? `<${b.type}>${content}</${b.type}>` : `<p>${content}</p>`
    }).join('')
}

// 上传
const imgUrl = ref('')
const beforeUpload = (file) => {
    // 针对上传的文件进行校验
    const isImage = file.type.startsWith('image/')
    const isLt5M = file.size / 1024 / 1024 < 5
    if (!isImage) {
        ElMessage.error('上传封面图片，请选择图片文件')
        return false
    }
    if (!isLt5M) {
        ElMessage.error('上传封面图片，图片大小不能超过5MB')
        return false
    }
    return true
}
const businessId = ref(null)
const handleUploadRequest = async ({ file }) => {
    // UUID生成
    businessId.value = crypto.randomUUID()
    try {
        const fileRes = await uploadFile(file, {
            businessId: businessId.value
        })
        // 拼接完整的图片地址
        imgUrl.value = fileBaseUrl + fileRes.filePath
        formData.coverImage = fileRes.filePath
    } catch (e) {
        ElMessage.error('封面上传失败，请重试')
    }
}

const handleRemove = () => {
    imgUrl.value = ''
    formData.coverImage = ''
}

// 富文本
const handleContentChange = (data) => {
    formData.content = data.html
}

const editorInstance = ref(null)
const handleEditorCreated = (editor) => {
    editorInstance.value = editor
    // 编辑
    if (formData.content && editor) {
        nextTick(() => {
            editor.setHtml(formData.content)
        })
    }
}

const btnPreview = ref(false)

// 提交
const formRef = ref()
const loading = ref(false)
const handleSubmit = () => {
    formRef.value.validate((valid) => {
        if (!valid) return
        loading.value = true
        const submitData = {
            ...formData,
            tags: (formData.tagArray || []).join(',')
        }
        delete submitData.tagArray

        const done = () => {
            loading.value = false
            emit('success')
        }
        const fail = () => {
            loading.value = false
        }

        if (!isEdit.value) {
            submitData.id = businessId.value
            createArticle(submitData).then(done).catch(fail)
        } else {
            updateArticle(props.article.id, submitData).then(done).catch(fail)
        }
    })
}
</script>
<style lang="scss" scoped>
.cover-placeholder {
    width: 200px;
    height: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8b949e;
    background: #f6f8fa;
}
.cover-image {
    width: 200px;
    height: 120px;
    display: block;
}

/* 文档导入区域 */
.doc-import {
    width: 100%;
    border: 1.5px dashed #c0c4cc;
    border-radius: 8px;
    padding: 22px 16px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s ease;
    background: #fafbfc;
    box-sizing: border-box;

    &:hover {
        border-color: #4A90E2;
        background: #f4f8ff;
    }

    &.is-dragover {
        border-color: #4A90E2;
        background: #eaf2ff;
        transform: scale(1.005);
    }

    .doc-input {
        display: none;
    }

    .doc-icon {
        font-size: 28px;
        color: #8b949e;
        margin-bottom: 8px;

        &.is-loading {
            color: #4A90E2;
            animation: doc-rotating 1s linear infinite;
        }

        &.success {
            color: #67c23a;
        }
    }

    .doc-tip {
        margin: 0;
        font-size: 14px;
        color: #303133;

        .doc-link {
            color: #4A90E2;
            font-weight: 500;
        }

        .doc-size {
            margin-left: 4px;
            color: #909399;
            font-size: 12px;
        }
    }

    .doc-sub {
        margin: 6px 0 0;
        font-size: 12px;
        color: #909399;
    }
}

@keyframes doc-rotating {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}
</style>
