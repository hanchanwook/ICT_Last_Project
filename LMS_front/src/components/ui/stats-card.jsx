import { Card, CardContent } from "./card"

export default function StatsCard({ 
  title, 
  value, 
  unit = "", 
  icon: Icon = null, 
  color = "#1ABC9C",
  onClick = null
}) {
  return (
    <Card 
      className={`text-center ${onClick ? 'cursor-pointer hover:shadow-lg transition-shadow' : ''}`}
      onClick={onClick}
    >
      <CardContent className="p-6">
        {Icon && (
          <div className="flex items-center justify-center mb-4">
            <div 
              className="p-3 rounded-full" 
              style={{ backgroundColor: `${color}20` }}
            >
              <Icon className="w-8 h-8" style={{ color }} />
            </div>
          </div>
        )}
        <div className="text-3xl font-bold mb-2" style={{ color }}>
          {value} {unit}
        </div>
        <div className="text-sm font-medium" style={{ color: "#2C3E50" }}>
          {title}
        </div>
      </CardContent>
    </Card>
  )
} 