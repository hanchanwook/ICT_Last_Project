export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <head>
        <title>LMSync App</title>
        <meta name="description" content="학원 관리 시스템" />
      </head>
      <body>{children}</body>
    </html>
  )
}
