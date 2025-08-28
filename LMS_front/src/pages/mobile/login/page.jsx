import { useState, useEffect } from "react"
import { Eye, EyeOff, ArrowLeft } from "lucide-react"
import MobileLoginForm from "@/components/auth/mobile-login-form"

export default function MobileLoginPage() {
  const [qrData, setQrData] = useState(null)

  useEffect(() => {
    // URL 파라미터에서 QR 코드 데이터를 가져옴
    const urlParams = new URLSearchParams(window.location.search)
    const data = urlParams.get('data')
    
    if (data) {
      setQrData(data)
      console.log('📱 모바일 로그인 - QR 데이터:', data)
    }
  }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <div className="bg-white shadow-sm border-b px-4 py-3">
        <div className="flex items-center">
          <button
            onClick={() => window.history.back()}
            className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <ArrowLeft className="w-6 h-6 text-gray-600" />
          </button>
          <h1 className="text-xl font-bold text-gray-900 ml-3">출석 로그인</h1>
        </div>
      </div>

      <div className="p-4">
        <div className="bg-white rounded-xl shadow-lg p-6">
          <div className="text-center mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">LMSync</h2>
            <p className="text-gray-600 text-lg">학원 관리 시스템</p>
            <p className="text-sm text-gray-500 mt-2">모바일 출석을 위해 로그인해주세요</p>
          </div>

          <MobileLoginForm qrData={qrData} />
        </div>
      </div>
    </div>
  )
} 