import axios from 'axios'

const request = axios.create({
  baseURL: '/shortlink',   // vite proxy 转发到后端
  timeout: 10000,
})

export const createShortLink = (data) => {
  return request.post('/create', data)
}