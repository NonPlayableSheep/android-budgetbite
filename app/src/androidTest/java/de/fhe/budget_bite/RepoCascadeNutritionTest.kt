package de.fhe.budget_bite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.fhe.budget_bite.storage.AppDatabase
import de.fhe.budget_bite.storage.DatabaseRepository
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemWithDetails
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlanWithItems
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredient
import de.fhe.budget_bite.storage.entity.recipe.RecipeWithRecipeIngredients
import de.fhe.budget_bite.util.HelperFunctions.getMealPlanWithItems
import de.fhe.budget_bite.util.LiveDataTestUtil.getOrAwaitValue
import de.fhe.budget_bite.utils.enums.ChangeStatus
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepoCascadeNutritionTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDatabase: AppDatabase
    private lateinit var repo: DatabaseRepository

    @Before
    fun setup() {
        testDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries() // Nur für Tests erlaubt!
            .build()
        repo = DatabaseRepository(testDatabase)
    }

    @After
    fun tearDown() {
        testDatabase.close()
    }

    /**
     * Tests the cascading update of nutrition information across ingredients, recipes, and meal plans.
     *
     * The test verifies the following:
     * 1. An initial ingredient is created with specific nutrition information and added to a recipe and a meal plan.
     * 2. The recipe and meal plan correctly reflect the ingredient's initial nutrition information.
     * 3. The ingredient's nutrition information is updated.
     * 4. The updated nutrition information cascades to the recipe and the meal plan, and the totals are updated correctly.
     */
    @Test
    fun item_ingredient_update_cascades_correctly() = runBlocking {
        // Step 1: Create initial ingredient with specific nutrition information
        val initialEmbeddedNutritionInfo = EmbeddedNutritionInfo(
            price = 5.0f,
            calories = 250.0f,
            carbohydratesInGrams = 50.0f,
            fatInGrams = 20.0f,
            proteinInGrams = 10.0f
        )
        val initialIngredientQuantity = 1
        val initialIngredient = Ingredient(
            identifier = "Test Ingredient",
            measurementValueEntered = initialIngredientQuantity,
            normalisedEmbeddedNutritionInfo = initialEmbeddedNutritionInfo
        )
        val insertedIngredient = repo.insertIngredient(initialIngredient).getOrAwaitValue()

        // Step 2: Create a recipe and add the ingredient to it
        val recipe = Recipe(
            identifier = "Test Recipe",
            embeddedNutritionInfoInTotal = initialEmbeddedNutritionInfo
        )
        val recipeIngredient = RecipeIngredient(
            ingredient = insertedIngredient,
            quantity = initialIngredientQuantity,
            sequenceIndex = 0,
            changeStatus = ChangeStatus.NEW
        )
        val recipeWithIngredient = RecipeWithRecipeIngredients(
            recipe = recipe,
            ingredients = listOf(recipeIngredient)
        )
        val insertedRecipe = repo.insertRecipeWithIngredients(recipeWithIngredient).getOrAwaitValue()

        // Step 3: Create a meal plan and add the recipe and ingredient to it
        val mealPlanNutritionInfo = EmbeddedNutritionInfo(
            price = initialEmbeddedNutritionInfo.price * 2,
            calories = initialEmbeddedNutritionInfo.calories * 2,
            carbohydratesInGrams = initialEmbeddedNutritionInfo.carbohydratesInGrams * 2,
            fatInGrams = initialEmbeddedNutritionInfo.fatInGrams * 2,
            proteinInGrams = initialEmbeddedNutritionInfo.proteinInGrams * 2
        )
        val mealPlan = MealPlan(
            identifier = "Test Meal Plan",
            embeddedNutritionInfoInTotal = mealPlanNutritionInfo
        )
        val mealType = MealType.BREAKFAST
        val mealItemIngredient = MealItemWithDetails(
            mealItemId = insertedIngredient.id,
            quantity = initialIngredientQuantity,
            sequenceIndex = 0,
            mealType = mealType,
            mealItemType = MealItemType.INGREDIENT,
            ingredient = insertedIngredient,
            changeStatus = ChangeStatus.NEW
        )
        val mealItemRecipe = MealItemWithDetails(
            mealItemId = insertedRecipe.recipe.id,
            quantity = 1,
            sequenceIndex = 1,
            mealType = mealType,
            mealItemType = MealItemType.RECIPE,
            recipe = insertedRecipe.recipe,
            changeStatus = ChangeStatus.NEW
        )
        val itemByType = mutableMapOf<MealType, List<MealItemWithDetails>>()
        itemByType[mealType] = listOf(mealItemIngredient, mealItemRecipe)
        val mealPlanWithItems = MealPlanWithItems(
            mealPlan,
            itemsByMealType = itemByType.toMap()
        )
        val insertedMealPlan = repo.insertMealPlanWithItems(mealPlanWithItems).getOrAwaitValue()

        // Step 4: Update Ingredient's Nutrition Information
        val updatedEmbeddedNutritionInfo = EmbeddedNutritionInfo(
            price = 10.0f, // Updated price
            calories = 500.0f, // Updated calories
            carbohydratesInGrams = 100.0f, // Updated carbohydrates
            fatInGrams = 40.0f, // Updated fat
            proteinInGrams = 20.0f // Updated protein
        )
        val updatedIngredient = insertedIngredient.copy(
            normalisedEmbeddedNutritionInfo = updatedEmbeddedNutritionInfo
        )
        repo.updateIngredient(updatedIngredient)

        // Step 5: Retrieve updated Recipe and Meal Plan
        val updatedRecipe = repo.getRecipeWithIngredients(insertedRecipe.recipe.id).getOrAwaitValue()
        val updatedMealPlan = repo.getMealPlanWithItems(insertedMealPlan.mealPlan.id).getOrAwaitValue()
        assertNotNull(updatedRecipe)

        // Assert: Recipe nutrition info should match the updated ingredient nutrition info
        assertEquals(updatedEmbeddedNutritionInfo.price, updatedRecipe.recipe.embeddedNutritionInfoInTotal.price, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.calories, updatedRecipe.recipe.embeddedNutritionInfoInTotal.calories, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.carbohydratesInGrams, updatedRecipe.recipe.embeddedNutritionInfoInTotal.carbohydratesInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.fatInGrams, updatedRecipe.recipe.embeddedNutritionInfoInTotal.fatInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.proteinInGrams, updatedRecipe.recipe.embeddedNutritionInfoInTotal.proteinInGrams, 0.01f)
        assertNotNull(updatedMealPlan)
        // Assert: MealPlan nutrition info should match 2x the updated ingredient nutrition info
        assertEquals(updatedEmbeddedNutritionInfo.price * 2, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.price, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.calories * 2, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.calories, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.carbohydratesInGrams * 2, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.carbohydratesInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.fatInGrams * 2, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.fatInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.proteinInGrams * 2, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.proteinInGrams, 0.01f)
    }

    /**
     * Tests the cascading deletion of an ingredient and its impact on associated recipes and meal plans.
     *
     * The test verifies the following:
     * 1. An initial ingredient is created with specific nutrition information and added to a recipe and a meal plan.
     * 2. The ingredient is deleted from the repository.
     * 3. The associated recipe and meal plan's nutrition information is updated correctly, reflecting the removal of the ingredient.
     */
    @Test
    fun item_ingredient_delete_cascades_correctly() = runBlocking {
        val initialEmbeddedNutritionInfo = EmbeddedNutritionInfo(
            price = 5.0f,
            calories = 250.0f,
            carbohydratesInGrams = 50.0f,
            fatInGrams = 20.0f,
            proteinInGrams = 10.0f
        )
        val initialIngredientQuantity = 1
        val initialIngredient = Ingredient(
            identifier = "Test Ingredient",
            measurementValueEntered = initialIngredientQuantity,
            normalisedEmbeddedNutritionInfo = initialEmbeddedNutritionInfo
        )
        val insertedIngredient = repo.insertIngredient(initialIngredient).getOrAwaitValue()
        val recipe = Recipe(
            identifier = "Test Recipe",
            embeddedNutritionInfoInTotal = initialEmbeddedNutritionInfo
        )
        val recipeIngredient = RecipeIngredient(
            ingredient = insertedIngredient,
            quantity = initialIngredientQuantity,
            sequenceIndex = 0,
            changeStatus = ChangeStatus.NEW
        )
        val recipeWithIngredient = RecipeWithRecipeIngredients(
            recipe = recipe,
            ingredients = listOf(recipeIngredient)
        )
        val insertedRecipe = repo.insertRecipeWithIngredients(recipeWithIngredient).getOrAwaitValue()
        val mealPlanNutritionInfo = EmbeddedNutritionInfo(
            price = initialEmbeddedNutritionInfo.price * 2,
            calories = initialEmbeddedNutritionInfo.calories * 2,
            carbohydratesInGrams = initialEmbeddedNutritionInfo.carbohydratesInGrams * 2,
            fatInGrams = initialEmbeddedNutritionInfo.fatInGrams * 2,
            proteinInGrams = initialEmbeddedNutritionInfo.proteinInGrams * 2
        )
        val mealPlan = MealPlan(
            identifier = "Test Meal Plan",
            embeddedNutritionInfoInTotal = mealPlanNutritionInfo
        )
        val mealType = MealType.BREAKFAST
        val mealItemIngredient = MealItemWithDetails(
            mealItemId = insertedIngredient.id,
            quantity = initialIngredientQuantity,
            sequenceIndex = 0,
            mealType = mealType,
            mealItemType = MealItemType.INGREDIENT,
            ingredient = insertedIngredient,
            changeStatus = ChangeStatus.NEW
        )
        val mealItemRecipe = MealItemWithDetails(
            mealItemId = insertedRecipe.recipe.id,
            quantity = 1,
            sequenceIndex = 1,
            mealType = mealType,
            mealItemType = MealItemType.RECIPE,
            recipe = insertedRecipe.recipe,
            changeStatus = ChangeStatus.NEW
        )
        val itemByType = mutableMapOf<MealType, List<MealItemWithDetails>>()
        itemByType[mealType] = listOf(mealItemIngredient, mealItemRecipe)
        val mealPlanWithItems = MealPlanWithItems(
            mealPlan,
            itemsByMealType = itemByType.toMap()
        )
        val insertedMealPlan = repo.insertMealPlanWithItems(mealPlanWithItems).getOrAwaitValue()

        repo.deleteIngredient(insertedIngredient)

        val updatedRecipe = repo.getRecipeWithIngredients(insertedRecipe.recipe.id).getOrAwaitValue()
        val updatedMealPlan = repo.getMealPlanWithItems(insertedMealPlan.mealPlan.id).getOrAwaitValue()
        assertNotEquals(insertedRecipe.recipe.embeddedNutritionInfoInTotal, updatedRecipe.recipe.embeddedNutritionInfoInTotal)
        assertNotEquals(insertedMealPlan.mealPlan.embeddedNutritionInfoInTotal, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal)
        assertEquals(0.0f, updatedRecipe.recipe.embeddedNutritionInfoInTotal.price, 0.01f)
        assertEquals(0.0f, updatedRecipe.recipe.embeddedNutritionInfoInTotal.calories, 0.01f)
        assertEquals(0.0f, updatedRecipe.recipe.embeddedNutritionInfoInTotal.carbohydratesInGrams, 0.01f)
        assertEquals(0.0f, updatedRecipe.recipe.embeddedNutritionInfoInTotal.fatInGrams, 0.01f)
        assertEquals(0.0f, updatedRecipe.recipe.embeddedNutritionInfoInTotal.proteinInGrams, 0.01f)
        assertEquals(0.0f, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.price, 0.01f)
        assertEquals(0.0f, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.calories, 0.01f)
        assertEquals(0.0f, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.carbohydratesInGrams, 0.01f)
        assertEquals(0.0f, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.fatInGrams, 0.01f)
        assertEquals(0.0f, updatedMealPlan.mealPlan.embeddedNutritionInfoInTotal.proteinInGrams, 0.01f)
    }

    /**
     * Tests the cascading update of a recipe and its impact on associated meal plans.
     *
     * The test verifies the following:
     * 1. A recipe with specific nutrition information is created and added to two meal plans with different multipliers.
     * 2. The recipe's nutrition information is updated.
     * 3. The associated meal plans' nutrition information is updated correctly, reflecting the updated recipe values.
     */
    @Test
    fun item_recipe_update_cascades_correctly() = runBlocking {
        val initialEmbeddedNutritionInfo = EmbeddedNutritionInfo(
            price = 5.0f,
            calories = 250.0f,
            carbohydratesInGrams = 50.0f,
            fatInGrams = 20.0f,
            proteinInGrams = 10.0f
        )
        val recipe = Recipe(
            identifier = "Test Recipe",
            embeddedNutritionInfoInTotal = initialEmbeddedNutritionInfo
        )
        val recipeWithIngredient = RecipeWithRecipeIngredients(
            recipe = recipe,
            ingredients = listOf()
        )
        val insertedRecipe = repo.insertRecipeWithIngredients(recipeWithIngredient).getOrAwaitValue()
        val mealPlan1 = getMealPlanWithItems(insertedRecipe.recipe, 1)
        val mealPlan2 = getMealPlanWithItems(insertedRecipe.recipe, 2)
        val insertedMealPlan1 = repo.insertMealPlanWithItems(mealPlan1).getOrAwaitValue()
        val insertedMealPlan2 = repo.insertMealPlanWithItems(mealPlan2).getOrAwaitValue()

        val updatedEmbeddedNutritionInfo = EmbeddedNutritionInfo(
            price = 10.0f, // Updated price
            calories = 500.0f, // Updated calories
            carbohydratesInGrams = 100.0f, // Updated carbohydrates
            fatInGrams = 40.0f, // Updated fat
            proteinInGrams = 20.0f // Updated protein
        )
        val updatedRecipe = insertedRecipe.recipe.copy(
            embeddedNutritionInfoInTotal = updatedEmbeddedNutritionInfo
        )
        val updatedRecipeWithIngredients = insertedRecipe.copy(
            recipe = updatedRecipe
        )
        repo.updateRecipeWithIngredients(updatedRecipeWithIngredients)

        val updatedMealPlan1 = repo.getMealPlanWithItems(insertedMealPlan1.mealPlan.id).getOrAwaitValue()
        val updatedMealPlan2 = repo.getMealPlanWithItems(insertedMealPlan2.mealPlan.id).getOrAwaitValue()
        // Assert that the nutrition infos of inserted and updated Meal Plans are different
        assertNotEquals(insertedMealPlan1.mealPlan.embeddedNutritionInfoInTotal, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal)
        assertNotEquals(insertedMealPlan2.mealPlan.embeddedNutritionInfoInTotal, updatedMealPlan2.mealPlan.embeddedNutritionInfoInTotal)
        // Assert for MealPlan1 (1x the updated recipe values)
        assertEquals(updatedEmbeddedNutritionInfo.price, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.price, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.calories, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.calories, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.carbohydratesInGrams, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.carbohydratesInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.fatInGrams, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.fatInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.proteinInGrams, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.proteinInGrams, 0.01f)
        // Assert for MealPlan2 (2x the updated recipe values)
        assertEquals(updatedEmbeddedNutritionInfo.price * 2, updatedMealPlan2.mealPlan.embeddedNutritionInfoInTotal.price, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.calories * 2, updatedMealPlan2.mealPlan.embeddedNutritionInfoInTotal.calories, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.carbohydratesInGrams * 2, updatedMealPlan2.mealPlan.embeddedNutritionInfoInTotal.carbohydratesInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.fatInGrams * 2, updatedMealPlan2.mealPlan.embeddedNutritionInfoInTotal.fatInGrams, 0.01f)
        assertEquals(updatedEmbeddedNutritionInfo.proteinInGrams * 2, updatedMealPlan2.mealPlan.embeddedNutritionInfoInTotal.proteinInGrams, 0.01f)
    }

    /**
     * Tests the cascading delete of a recipe and its impact on associated meal plans only.
     *
     * The test verifies:
     * 1. A recipe with specific nutrition information is created and added to only one of two meal plans.
     * 2. The recipe is deleted along with its associated data.
     * 3. The associated meal plans' nutrition information is updated correctly:
     *    - MealPlan1 is updated to reflect the removal of the recipe.
     *    - MealPlan2 retains its initial nutrition info as it is not affected by the deletion.
     */
    @Test
    fun item_recipe_delete_cascades_correctly() = runBlocking {
        val initialEmbeddedNutritionInfo = EmbeddedNutritionInfo(
            price = 5.0f,
            calories = 250.0f,
            carbohydratesInGrams = 50.0f,
            fatInGrams = 20.0f,
            proteinInGrams = 10.0f
        )
        val recipe = Recipe(
            identifier = "Test Recipe",
            embeddedNutritionInfoInTotal = initialEmbeddedNutritionInfo
        )
        val recipeWithIngredient = RecipeWithRecipeIngredients(
            recipe = recipe,
            ingredients = listOf()
        )
        val insertedRecipe = repo.insertRecipeWithIngredients(recipeWithIngredient).getOrAwaitValue()
        val mealPlan1 = getMealPlanWithItems(insertedRecipe.recipe, 1)
        val mealPlan = MealPlan(
            identifier = "Test Meal Plan",
            embeddedNutritionInfoInTotal = initialEmbeddedNutritionInfo
        )
        val mealPlan2 = MealPlanWithItems(
            mealPlan = mealPlan
        )
        val insertedMealPlan1 = repo.insertMealPlanWithItems(mealPlan1).getOrAwaitValue()
        val insertedMealPlan2 = repo.insertMealPlanWithItems(mealPlan2).getOrAwaitValue()

        repo.deleteRecipeWithIngredients(insertedRecipe)

        val updatedMealPlan1 = repo.getMealPlanWithItems(insertedMealPlan1.mealPlan.id).getOrAwaitValue()
        val updatedMealPlan2 = repo.getMealPlanWithItems(insertedMealPlan2.mealPlan.id).getOrAwaitValue()
        // Assert that the nutrition infos of inserted and updated Meal Plans are different
        assertNotEquals(insertedMealPlan1.mealPlan.embeddedNutritionInfoInTotal, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal)
        assertEquals(initialEmbeddedNutritionInfo, updatedMealPlan2.mealPlan.embeddedNutritionInfoInTotal)
        // Assert for updatedMealPlan1 (all values should be 0 after recipe deletion)
        assertEquals(0.0f, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.price, 0.01f)
        assertEquals(0.0f, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.calories, 0.01f)
        assertEquals(0.0f, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.carbohydratesInGrams, 0.01f)
        assertEquals(0.0f, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.fatInGrams, 0.01f)
        assertEquals(0.0f, updatedMealPlan1.mealPlan.embeddedNutritionInfoInTotal.proteinInGrams, 0.01f)
    }
}