# 🌿 PlantPal  
**Mobile Plant Care Assistant**

## 📱 Overview  
PlantPal is a mobile-first Android application designed to help users take better care of their indoor plants through **simple, personalized, and adaptive guidance**.
New plant owners often struggle with watering schedules and light placement because most care guides are static (e.g., “water once a week”). PlantPal solves this by combining **plant-specific data, user logs, and environmental awareness** to provide smarter recommendations.

## 🎯 Problem  
Plant care resources today are:
- Static  
- Generic  
- Not context-aware  

This leads to:
- Overwatering / underwatering  
- Poor plant placement  
- Plants dying despite following instructions  

## 💡 Solution  
PlantPal provides:
- 🌱 Personalized plant care profiles  
- 💧 Watering logs and reminders  
- 📊 Adaptive care guidance  
- 📱 Offline access to instructions  
- 🧠 Simple decision support in real time  

## ✨ Features  

### 1. Add a Plant  
- Enter plant name, species, and location  
- Customize nickname  
- Generate a care profile  

### 2. Plant Dashboard  
- View all saved plants  
- Persistent local storage (no re-entry needed)  

### 3. Watering Log  
- Log watering events  
- Track history  
- Update last watered status  

### 4. Offline Care Instructions  
- Stored locally using Room  
- Accessible without internet  

### 5. Plant Detail View  
- Light requirements  
- Watering schedule  
- Care instructions  
- Watering history  

## AI Disclosure
We used AI as a support tool for debugging, planning, refactoring, and improving the UI, while still making all the final design and architecture decisions ourselves.

Firstly, we used AI to help set up GitHub Actions for CI, so builds and tests run automatically before merging. This helped us collaborate more safely.

Additionally, when plants weren’t being saved to the Room database, we initially thought the issue was in Room or the DAO layer. With AI’s help, we traced the problem across the full data flow and realized that addPlant() was exiting early because currentUserId was null. This was due to our temporary authentication setup, where the login UI didn’t actually create or load a user in the database. We then fixed this by making sure a user is created or retrieved before saving a plant.

We didn’t accept every suggestion, though. At one point, AI suggested using fallback preview data for the dashboard, but we rejected that because it conflicted with our architecture. We wanted Perenual to be used only for search, and Room to remain the single source of truth for saved plants.

We also used AI to help us clean up our code structure. Early on, too much UI was inside MainActivity, so we decided to split the app into separate screens like HomeDashboardScreen, ProfileScreen, AuthScreens, and PlantDetailScreen. This made the project easier to manage and work on as a team.

On the design side, we used AI to help implement a modern green-and-white theme with cleaner typography. It helped translate our ideas into Compose theme files instead of hard-coding styles everywhere. We kept things simple and only used what matched our vision.

Overall, AI helped us move faster and debug more effectively, but we made sure to stay in control of the decisions and direction of the project.
