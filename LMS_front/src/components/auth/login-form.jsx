import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { useNavigate } from "react-router-dom";
import { http } from "@/components/auth/http";

export default function LoginForm() {
  const [loginData, setLoginData] = useState({
    username: "",
    password: "",
  });

  const navigate = useNavigate();

  const getDefaultPage = (role) => {
    switch (role) {
      case "admin":
      case "director":
      case "staff":
      case "instructor":
      case "student":
        return "/dashboard";
      default:
        return "/dashboard";
    }
  };

  const convertRole = (role) => {
    return role.replace("ROLE_", "").toLowerCase();
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const response = await http.post("/api/auth/login",
        {
          username: loginData.username,
          password: loginData.password,
        },
        {
          withCredentials: true,
        }
      );

      if (response.status === 200) {
        const { data } = response.data;

        if (!data) {
          alert("사용자 정보를 찾을 수 없습니다.");
          return;
        }

        const { name, role, memberId, accessToken } = data;
        const convertedRole = convertRole(role);

        localStorage.setItem(
          "currentUser",
          JSON.stringify({
            name,
            role: convertedRole,
            memberId,
          })
        );
        if (accessToken) {
          localStorage.setItem("token", accessToken);
        }

        navigate(getDefaultPage(role));
      }
    } catch (error) {
      if (error.response) {
        // 서버가 응답했지만 에러 코드인 경우
        switch (error.response.status) {
          case 400:
            alert("잘못된 요청입니다. 입력 값을 확인하세요.");
            break;
          case 401:
            alert("아이디 또는 비밀번호가 올바르지 않습니다.");
            break;
          case 404:
            alert("사용자를 찾을 수 없습니다.");
            break;
          case 500:
            alert("서버 오류가 발생했습니다. 잠시 후 다시 시도하세요.");
            break;
          default:
            alert(
              `알 수 없는 오류가 발생했습니다. (코드: ${error.response.status})`
            );
        }
      } else if (error.request) {
        // 요청이 전송되었지만 응답이 없는 경우
        alert("서버 응답이 없습니다. 네트워크 상태를 확인해주세요.");
      } else {
        // 그 외의 에러 (코드 문제 등)
        alert("요청 처리 중 오류가 발생했습니다.");
      }
    }
  };

  return (
    <form onSubmit={handleLogin} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="loginId">아이디를 입력하세요</Label>
        <input
          id="loginId"
          type="text"
          placeholder="아이디를 입력하세요"
          value={loginData.username}
          onChange={(e) =>
            setLoginData({ ...loginData, username: e.target.value })
          }
          className="w-full px-3 py-2 border border-gray-300 rounded-md 
                     placeholder-gray-400 focus:outline-none focus:border-black"
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="loginPassword">비밀번호를 입력하세요</Label>
        <input
          id="loginPassword"
          type="password"
          placeholder="비밀번호를 입력하세요"
          value={loginData.password}
          onChange={(e) =>
            setLoginData({ ...loginData, password: e.target.value })
          }
          className="w-full px-3 py-2 border border-gray-300 rounded-md 
                     placeholder-gray-400 focus:outline-none focus:border-black"
          required
        />
      </div>

      <Button
        type="submit"
        className="w-full text-white font-medium"
        style={{ backgroundColor: "#1ABC9C" }}
      >
        로그인
      </Button>
    </form>
  );
}
