import { Outlet } from "react-router-dom"
import Footer from "./footer"

function RootLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

export default RootLayout 