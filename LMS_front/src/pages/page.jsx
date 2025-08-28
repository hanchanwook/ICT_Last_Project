import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import LoginForm from "@/components/auth/login-form";
import RegisterForm from "@/components/auth/register-form";

export default function LoginPage() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  // URL 파라미터에서 tab=login 강제 반영
  const [tabValue, setTabValue] = useState("login");

  useEffect(() => {
    const tab = params.get("tab");
    if (tab) {
      setTabValue(tab); // PasswordResetForm에서 navigate("/login?tab=login") 했을 때 반영됨
    } else {
      setTabValue("login");
    }
  }, [location.search]);

  return (
    <div
      className="min-h-screen flex items-center justify-center"
      style={{ backgroundColor: "#2C3E50" }}
    >
      <Card className="w-full max-w-md mx-4">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold" style={{ color: "#1ABC9C" }}>
            LMSync
          </CardTitle>
          <p className="text-sm" style={{ color: "#95A5A6" }}>
            학원 관리 시스템
          </p>
        </CardHeader>
        <CardContent>
          <Tabs value={tabValue} onValueChange={setTabValue} className="w-full">
            <TabsList className="grid w-full grid-cols-2 mb-6">
              <TabsTrigger
                value="login"
                className="data-[state=active]:text-[#1ABC9C]"
              >
                로그인
              </TabsTrigger>
              <TabsTrigger
                value="register"
                className="data-[state=active]:text-[#1ABC9C]"
              >
                계정찾기
              </TabsTrigger>
            </TabsList>

            <TabsContent value="login">
              <LoginForm />
            </TabsContent>

            <TabsContent value="register">
              <RegisterForm />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
