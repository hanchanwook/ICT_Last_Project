import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Label } from "@/components/ui/label";
import { http } from "@/components/auth/http";

export default function PasswordResetForm() {
  const [step, setStep] = useState(1);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const navigate = useNavigate();

  // 1단계: 사용자 인증 + 인증번호 발송
  const handleSendCode = async (e) => {
    e.preventDefault();
    try {
      await http.post("/api/find/user-check", {
        name,
        email,
      });
      await http.post("/api/find/send-code", { email });

      alert("인증번호가 이메일로 발송되었습니다.");
      setStep(2);
    } catch (err) {
      alert(
        err.response?.status === 404
          ? "사용자를 찾을 수 없습니다."
          : "서버 오류가 발생했습니다."
      );
    }
  };

  // 2단계: 인증번호 확인
  const handleVerifyCode = async (e) => {
    e.preventDefault();
    try {
      await http.post("/api/find/verify-code", { email, code });
      alert("인증 성공!");
      setStep(3);
    } catch (err) {
      alert(
        err.response?.status === 400
          ? "인증번호가 올바르지 않습니다."
          : "서버 오류가 발생했습니다."
      );
    }
  };

  // 3단계: 비밀번호 재설정
  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }
    try {
      await http.post("/api/find/reset-password", {
        email,
        newPassword,
      });
      alert("비밀번호가 성공적으로 변경되었습니다.");
      navigate("/login?tab=login");
    } catch (err) {
      alert("비밀번호 변경에 실패했습니다.");
    }
  };

  return (
    <>
      {step === 1 && (
        <form onSubmit={handleSendCode} className="space-y-4">
          <p className="text-sm text-center mb-4" style={{ color: "#95A5A6" }}>
            이름과 이메일을 입력하세요
          </p>
          <div className="space-y-2">
            <Label>이름</Label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder="이름 입력"
              required
            />
          </div>
          <div className="space-y-2">
            <Label>이메일</Label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder="이메일 입력"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 px-4 text-white rounded-md hover:bg-green-600"
            style={{ backgroundColor: "#1ABC9C" }}
          >
            인증번호 전송
          </button>
        </form>
      )}

      {step === 2 && (
        <form onSubmit={handleVerifyCode} className="space-y-4">
          <p className="text-sm text-center mb-4" style={{ color: "#95A5A6" }}>
            이메일로 받은 인증번호를 입력하세요
          </p>
          <div className="space-y-2">
            <Label>인증번호</Label>
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder="인증번호 입력"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 px-4 text-white rounded-md hover:bg-green-600"
            style={{ backgroundColor: "#1ABC9C" }}
          >
            인증하기
          </button>
        </form>
      )}

      {step === 3 && (
        <form onSubmit={handleResetPassword} className="space-y-4">
          <p className="text-sm text-center mb-4" style={{ color: "#95A5A6" }}>
            새 비밀번호를 입력하세요
          </p>
          <div className="space-y-2">
            <Label>새 비밀번호</Label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder="새 비밀번호 입력"
              required
            />
          </div>
          <div className="space-y-2">
            <Label>비밀번호 확인</Label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder="비밀번호 확인"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 px-4 text-white rounded-md hover:bg-green-600"
            style={{ backgroundColor: "#1ABC9C" }}
          >
            비밀번호 변경
          </button>
        </form>
      )}
    </>
  );
}
