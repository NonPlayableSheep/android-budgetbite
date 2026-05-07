## Table of Contents
- [Project Description & Features](#project-description--features)
- [Technologies](#technologies)
- [Architecture & Patterns](#architecture--patterns)
- [Setup & Start](#setup--start)
- [Environment](#environment)
- [Notes](#notes)
   - [Thesis Preparation Extensions](#thesis-preparation-extensions)
   - [Run Requirements](#run-requirements)

## Project Description & Features
BudgetBite is an Android meal planner application with integrated budgeting and nutrition tracking features.  
The application allows users to manage ingredients, recipes, and meal plans. Ingredients can be assigned to recipes, while recipes and ingredients can be organized into meal plans.  
For each ingredient, nutritional values and pricing information can be configured. Nutritional values and total costs of recipes and meal plans are automatically calculated based on their components.

## Technologies
- Kotlin
- XML-based Android UI
- Android SDK
- Room
- Hilt
- Kotlin Coroutines
- Android Navigation Component
- Material Design
- JUnit
- Espresso

## Architecture & Patterns
- MVVM Architecture
- Dependency Injection Pattern
- Repository Pattern

## Setup & Start
Clone the repository and open the project in Android Studio.  
Build and run the application on an Android emulator or physical device.

## Environment
- Android Studio
- Android API Level 33
- Tested on Android 13

## Notes
### Thesis Preparation Extensions
The original practical project was later extended in preparation for a subsequent bachelor's thesis.  
Additional components such as the benchmark module and Jetpack Compose integrations were introduced as part of the thesis-related work and are therefore outside the scope of the original practical project.  
Within the context of the practical project, these additions can be ignored.

### Run Requirements
Copy `.env.example` to `.env` and configure the required variables.