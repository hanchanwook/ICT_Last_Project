import { useNavigate } from "react-router-dom"
import { useEffect } from "react"

export default function CoursesPage() {
  const navigate = useNavigate()

  useEffect(() => {
    // /courses로 접근 시 /courses/course(과정 리스트)로 자동 리다이렉트
    navigate("/courses/course")
  }, [navigate])

  return null
}

