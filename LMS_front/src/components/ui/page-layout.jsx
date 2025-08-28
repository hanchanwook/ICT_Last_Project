import Header from "../layout/header"

export default function PageLayout({ 
  children, 
  currentPage = "", 
  userRole = "student", 
  userName = "사용자",
  title = "LMSync" 
}) {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header 
        currentPage={currentPage} 
        userRole={userRole} 
        userName={userName} 
      />
      {children}
    </div>
  )
} 