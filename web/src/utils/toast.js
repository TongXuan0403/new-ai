let container = null
let timer = null

export function toast(message) {
  if (!container) {
    container = document.createElement('div')
    container.className = 'toast'
    document.body.appendChild(container)
  }
  container.textContent = message
  container.classList.remove('hidden')
  clearTimeout(timer)
  timer = setTimeout(() => container.classList.add('hidden'), 2600)
}
