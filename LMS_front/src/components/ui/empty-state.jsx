import { Card, CardContent } from "./card"

export default function EmptyState({ 
  icon: Icon, 
  title, 
  description, 
  action = null 
}) {
  return (
    <Card>
      <CardContent className="p-12 text-center">
        {Icon && (
          <div className="flex items-center justify-center mb-6">
            <div className="p-4 rounded-full bg-gray-100">
              <Icon className="w-12 h-12 text-gray-400" />
            </div>
          </div>
        )}
        <h3 className="text-xl font-semibold mb-2" style={{ color: "#2C3E50" }}>
          {title}
        </h3>
        <p className="text-gray-600 mb-6 max-w-md mx-auto">
          {description}
        </p>
        {action}
      </CardContent>
    </Card>
  )
} 