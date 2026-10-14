const API_URL = import.meta.env.VITE_API_URL || '/api'

let credentials = sessionStorage.getItem('campusHubCredentials') || ''

export function setCredentials(username, password) {
  credentials = btoa(`${username}:${password}`)
  sessionStorage.setItem('campusHubCredentials', credentials)
}

export function clearCredentials() {
  credentials = ''
  sessionStorage.removeItem('campusHubCredentials')
}

export async function api(path, options = {}) {
  const headers = { ...options.headers }
  if (options.body) headers['Content-Type'] = 'application/json'
  if (credentials) headers.Authorization = `Basic ${credentials}`

  const response = await fetch(`${API_URL}${path}`, { ...options, headers })
  if (response.status === 204) return null

  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'Không thể kết nối tới hệ thống')
  return body
}
