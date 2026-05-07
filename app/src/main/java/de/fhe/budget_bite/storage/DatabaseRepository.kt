package de.fhe.budget_bite.storage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.switchMap
import androidx.room.Transaction
import com.github.javafaker.Faker
import de.fhe.budget_bite.storage.dao.IngredientDao
import de.fhe.budget_bite.storage.dao.MealPlanDao
import de.fhe.budget_bite.storage.dao.RecipeDao
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.meal_plan.MealItem
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemWithDetails
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlanWithItems
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredient
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredientRef
import de.fhe.budget_bite.storage.entity.recipe.RecipeWithIngredients
import de.fhe.budget_bite.storage.entity.recipe.RecipeWithRecipeIngredients
import de.fhe.budget_bite.utils.enums.ChangeStatus
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType
import de.fhe.budget_bite.utils.enums.MeasurementType
import de.fhe.budget_bite.utils.helper.NutritionCalculator.setNormalisedNutritionInfos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DatabaseRepository @Inject constructor(
    private val appDatabase: AppDatabase
) {
    private val ingredientDao: IngredientDao = appDatabase.ingredientDao()
    private val recipeDao: RecipeDao = appDatabase.recipeDao()
    private val mealPlanDao: MealPlanDao = appDatabase.mealPlanDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private fun isTestDatabase(): Boolean {
        // because TestDatabase is inMemory
        return appDatabase.openHelper.databaseName == null
    }

    init {
        if (!isTestDatabase()) {
            // init test data
            val testDataNumberIngredients = 20 // 200
            val faker = Faker.instance()

            repositoryScope.launch {
                val firstIngredient = ingredientDao.checkForEmptyTable()

                if (firstIngredient == null) {
                    val ingredients = mutableListOf<Ingredient>()

                    for (i in 1..testDataNumberIngredients) {
                        val ingredient = Ingredient(
                            identifier = faker.food().ingredient(),
                            measurementType = getRandomMeasurementUnit(i),
                            measurementValueEntered = getRandomInt(),
                            embeddedNutritionInfo = getRandomEmbeddedNutritionInfo()
                        )
                        setNormalisedNutritionInfos(ingredient)
                        ingredients.add(ingredient)
                    }

                    ingredientDao.insertTestIngredients(ingredients.toList())
                    createTestRecipes(faker, ingredientDao.getAllTestIngredients())
                }
            }
        }
    }

    private suspend fun createTestRecipes(faker: Faker, ingredients: List<Ingredient>) {
        for (i in 1..4) {
            val selectedIngredients = mutableSetOf<Ingredient>()
            val recipeIngredients = mutableListOf<RecipeIngredient>()
            val recipeNutritionInfo = EmbeddedNutritionInfo()

            for (j in 0..2) {
                var ingredient: Ingredient
                do {
                    ingredient = ingredients.random()
                } while (selectedIngredients.contains(ingredient)) // Prüfen, ob die Zutat bereits ausgewählt wurde
                selectedIngredients.add(ingredient)

                val quantity = getRandomInt()

                ingredient.normalisedEmbeddedNutritionInfo.let {
                    recipeNutritionInfo.apply {
                        calories += it.calories * quantity
                        price += it.price * quantity
                        carbohydratesInGrams += it.carbohydratesInGrams * quantity
                        fatInGrams += it.fatInGrams * quantity
                        proteinInGrams += it.proteinInGrams * quantity
                    }
                }

                val recipeIngredient = RecipeIngredient(
                    ingredient,
                    quantity,
                    j
                )
                recipeIngredients.add(recipeIngredient)
            }

            val recipe = Recipe(
                identifier = faker.food().dish(),
                embeddedNutritionInfoInTotal = recipeNutritionInfo
            )

            val recipeWithRecipeIngredients = RecipeWithRecipeIngredients(
                recipe,
                recipeIngredients.toList()
            )

            insertRecipeWithIngredients(recipeWithRecipeIngredients)
        }
        createTestMealPlans(faker, ingredients, recipeDao.getAllTestRecipes())
    }

    private suspend fun createTestMealPlans(faker: Faker, ingredients: List<Ingredient>, recipes: List<Recipe>) {
        for (i in 1..4) {
            val mealPlanNutritionInfo = EmbeddedNutritionInfo()
            val mealTypeItems = MealPlanWithItems.initEmptyItemsByMealType().toMutableMap()

            for (mealType in MealType.entries) {
                val itemList = mutableListOf<MealItemWithDetails>()
                for (j in 0..2) {
                    val quantity = getRandomInt()
                    val mealItemWithDetails: MealItemWithDetails
                    val mealItemInfo: EmbeddedNutritionInfo
                    if (j == 0) {
                        val recipe = recipes.random()
                        mealItemInfo = recipe.embeddedNutritionInfoInTotal
                        mealItemWithDetails = MealItemWithDetails(
                            mealItemId = recipe.id,
                            quantity = quantity,
                            sequenceIndex = j,
                            mealType = mealType,
                            mealItemType = MealItemType.RECIPE,
                            recipe = recipe,
                            changeStatus = ChangeStatus.NEW
                        )
                    } else {
                        val ingredient = ingredients.random()
                        mealItemInfo = ingredient.normalisedEmbeddedNutritionInfo
                        mealItemWithDetails = MealItemWithDetails(
                            mealItemId = ingredient.id,
                            quantity = quantity,
                            sequenceIndex = j,
                            mealType = mealType,
                            mealItemType = MealItemType.INGREDIENT,
                            ingredient = ingredient,
                            changeStatus = ChangeStatus.NEW
                        )
                    }

                    mealItemInfo.let {
                        mealPlanNutritionInfo.apply {
                            calories += it.calories * quantity
                            price += it.price * quantity
                            carbohydratesInGrams += it.carbohydratesInGrams * quantity
                            fatInGrams += it.fatInGrams * quantity
                            proteinInGrams += it.proteinInGrams * quantity
                        }
                    }

                    itemList.add(mealItemWithDetails)
                }
                mealTypeItems[mealType] = itemList
            }

            val mealPlan = MealPlan(
                identifier = faker.cat().name(),
                embeddedNutritionInfoInTotal = mealPlanNutritionInfo
            )
            val mealPlanWithItems = MealPlanWithItems(
                mealPlan,
                mealTypeItems
            )
            insertMealPlanWithItems(mealPlanWithItems)
        }
    }

    private fun getRandomInt() = Random.nextInt(1, 3)
    private fun getRandomFloat() = Random.nextInt(5, 25).toFloat()
    private fun getRandomEmbeddedNutritionInfo() = EmbeddedNutritionInfo(
        getRandomFloat(),
        getRandomFloat(),
        getRandomFloat(),
        getRandomFloat(),
        getRandomFloat()
    )
    private fun getRandomMeasurementUnit(iterator: Int) = when(iterator % 3) {
        0 -> MeasurementType.QUANTITY
        1 -> MeasurementType.VOLUME
        else -> MeasurementType.WEIGHT
    }

    suspend fun insertIngredient(ingredient: Ingredient): LiveData<Ingredient> {
        val insertedIngredientId = ingredientDao.insertIngredient(ingredient)
        return getIngredientById(insertedIngredientId)
    }

    @Transaction
    suspend fun updateIngredient(ingredient: Ingredient) {
        // get ingredient
        val newInfo = ingredient.normalisedEmbeddedNutritionInfo
        val oldInfo = ingredientDao.getIngredientById(ingredient.id).normalisedEmbeddedNutritionInfo

        // should happen here, so mealItem uses updated values
        ingredientDao.updateIngredient(ingredient)

        if (isNutritionInfoChanged(oldInfo, newInfo)) {
            // update recipes and plans
            updateRecipeAndMealPlanInfo(ingredient.id, oldInfo, newInfo)
        }
    }

    private fun isNutritionInfoChanged(oldInfo: EmbeddedNutritionInfo, newInfo: EmbeddedNutritionInfo): Boolean {
        return oldInfo.price != newInfo.price ||
                oldInfo.calories != newInfo.calories ||
                oldInfo.fatInGrams != newInfo.fatInGrams ||
                oldInfo.carbohydratesInGrams != newInfo.carbohydratesInGrams ||
                oldInfo.proteinInGrams != newInfo.proteinInGrams
    }

    private suspend fun updateRecipeAndMealPlanInfo(ingredientId: Long, oldInfo: EmbeddedNutritionInfo, newInfo: EmbeddedNutritionInfo) {
        val recipeIngredientRefs = recipeDao.getRecipeIngredientRefsByIngredient(ingredientId)
        val recipesWithIngredient = recipeDao.getRecipesByIngredient(ingredientId)

        val oldRecipeNutritionInfo = mutableMapOf<Long, EmbeddedNutritionInfo>()
        val newRecipeNutritionInfo = mutableMapOf<Long, EmbeddedNutritionInfo>()

        val refMap = recipeIngredientRefs.associateBy { it.recipeId }
        recipesWithIngredient.forEach { recipe ->
            oldRecipeNutritionInfo[recipe.id] = recipe.embeddedNutritionInfoInTotal.copy()

            // supposedly more efficient than firstOrNull
            refMap[recipe.id]?.let { correspondingRef ->
                updateIngredientInRecipeInfo(recipe, oldInfo, newInfo, correspondingRef.quantity)
            }

            newRecipeNutritionInfo[recipe.id] = recipe.embeddedNutritionInfoInTotal
        }
        recipeDao.updateRecipes(recipesWithIngredient)
        
        updateAssociatedMealPlanNutritionInfo(
            ingredientId,
            oldInfo,
            newInfo,
            oldRecipeNutritionInfo,
            newRecipeNutritionInfo
        )
    }

    private fun updateIngredientInRecipeInfo(recipe: Recipe, oldIngredientInfo: EmbeddedNutritionInfo, newIngredientInfo: EmbeddedNutritionInfo, quantity: Int) {
        recipe.embeddedNutritionInfoInTotal.apply {
            // Subtract the old ingredient info first
            calories -= oldIngredientInfo.calories * quantity
            price -= oldIngredientInfo.price * quantity
            carbohydratesInGrams -= oldIngredientInfo.carbohydratesInGrams * quantity
            fatInGrams -= oldIngredientInfo.fatInGrams * quantity
            proteinInGrams -= oldIngredientInfo.proteinInGrams * quantity

            // Then, add the new ingredient info
            calories += newIngredientInfo.calories * quantity
            price += newIngredientInfo.price * quantity
            carbohydratesInGrams += newIngredientInfo.carbohydratesInGrams * quantity
            fatInGrams += newIngredientInfo.fatInGrams * quantity
            proteinInGrams += newIngredientInfo.proteinInGrams * quantity
        }
    }

    private suspend fun updateAssociatedMealPlanNutritionInfo(
        ingredientId: Long,
        oldIngredientNutritionInfo: EmbeddedNutritionInfo,
        newIngredientNutritionInfo: EmbeddedNutritionInfo,
        oldNutritionInfosByIngredientRecipeIds: Map<Long, EmbeddedNutritionInfo>,
        newNutritionInfosByIngredientRecipeIds: Map<Long, EmbeddedNutritionInfo>
    ) {
        val mealItemsWithIngredient = mealPlanDao.getMealItemsByIngredientId(ingredientId)
        val mealItemsWithRecipes = mealPlanDao.getMealItemsByRecipeIds(oldNutritionInfosByIngredientRecipeIds.keys.toList())

        val allMealItems = mealItemsWithIngredient + mealItemsWithRecipes
        val mealItemsByPlanId = allMealItems.groupBy { it.mealPlanId }

        val mealPlanIds = mealItemsByPlanId.keys.toList()
        val mealPlans = mealPlanDao.getMealPlansByIds(mealPlanIds)

        mealPlans.forEach { mealPlan ->
            val mealItems = mealItemsByPlanId[mealPlan.id] ?: emptyList()

            val oldNutritionInfoSum = calculateNutritionSumForMealItems(
                mealItems,
                oldIngredientNutritionInfo,
                oldNutritionInfosByIngredientRecipeIds
            )
            val newNutritionInfoSum = calculateNutritionSumForMealItems(
                mealItems,
                newIngredientNutritionInfo,
                newNutritionInfosByIngredientRecipeIds
            )

            updateMealPlanWithNutritionDiff(mealPlan, oldNutritionInfoSum, newNutritionInfoSum)
        }

        mealPlanDao.updateMealPlans(mealPlans)
    }

    private fun calculateNutritionSumForMealItems(
        mealItems: List<MealItem>,
        ingredientNutritionInfo: EmbeddedNutritionInfo,
        nutritionInfosByIngredientRecipeIds: Map<Long, EmbeddedNutritionInfo>
    ): EmbeddedNutritionInfo {
        var totalCalories = 0f
        var totalCarbohydrates = 0f
        var totalFat = 0f
        var totalProtein = 0f
        var totalPrice = 0f

        for (mealItem in mealItems) {
            val quantity = mealItem.quantity
            when (mealItem.mealItemType) {
                MealItemType.INGREDIENT -> {
                    totalCalories += ingredientNutritionInfo.calories * quantity
                    totalCarbohydrates += ingredientNutritionInfo.carbohydratesInGrams * quantity
                    totalFat += ingredientNutritionInfo.fatInGrams * quantity
                    totalProtein += ingredientNutritionInfo.proteinInGrams * quantity
                    totalPrice += ingredientNutritionInfo.price * quantity
                }
                MealItemType.RECIPE -> {
                    nutritionInfosByIngredientRecipeIds[mealItem.mealItemId]?.let { recipeNutritionInfo ->
                        totalCalories += recipeNutritionInfo.calories * quantity
                        totalCarbohydrates += recipeNutritionInfo.carbohydratesInGrams * quantity
                        totalFat += recipeNutritionInfo.fatInGrams * quantity
                        totalProtein += recipeNutritionInfo.proteinInGrams * quantity
                        totalPrice += recipeNutritionInfo.price * quantity
                    }
                }
            }
        }

        return EmbeddedNutritionInfo(
            calories = totalCalories,
            carbohydratesInGrams = totalCarbohydrates,
            fatInGrams = totalFat,
            proteinInGrams = totalProtein,
            price = totalPrice
        )
    }

    private fun updateMealPlanWithNutritionDiff(mealPlan: MealPlan, oldInfo: EmbeddedNutritionInfo, newInfo: EmbeddedNutritionInfo) {
        mealPlan.embeddedNutritionInfoInTotal.apply {
            calories += (newInfo.calories - oldInfo.calories)
            carbohydratesInGrams += (newInfo.carbohydratesInGrams - oldInfo.carbohydratesInGrams)
            fatInGrams += (newInfo.fatInGrams - oldInfo.fatInGrams)
            proteinInGrams += (newInfo.proteinInGrams - oldInfo.proteinInGrams)
            price += (newInfo.price - oldInfo.price)
        }
    }

    @Transaction
    suspend fun deleteIngredient(ingredient: Ingredient) {
        // 1. Retrieve all RecipeIngredientRefs and recipes that include the ingredient
        val recipeIngredientRefs = recipeDao.getRecipeIngredientRefsByIngredient(ingredient.id)
        val recipesWithIngredient = recipeDao.getRecipesByIngredient(ingredient.id)

        val oldRecipeNutritionInfo = mutableMapOf<Long, EmbeddedNutritionInfo>()
        val newRecipeNutritionInfo = mutableMapOf<Long, EmbeddedNutritionInfo>()

        val refMap = recipeIngredientRefs.associateBy { it.recipeId }
        recipesWithIngredient.forEach { recipe ->
            oldRecipeNutritionInfo[recipe.id] = recipe.embeddedNutritionInfoInTotal.copy()

            // supposedly more efficient than firstOrNull
            refMap[recipe.id]?.let { correspondingRef ->
                subtractIngredientFromRecipeInfo(
                    recipe,
                    ingredient.normalisedEmbeddedNutritionInfo,
                    correspondingRef.quantity
                )
            }

            newRecipeNutritionInfo[recipe.id] = recipe.embeddedNutritionInfoInTotal
        }

        recipeDao.deleteRecipeIngredientRefs(recipeIngredientRefs)
        recipeDao.updateRecipes(recipesWithIngredient)

        val persistedIngredientNutritionInfo = ingredientDao.getIngredientById(ingredient.id).normalisedEmbeddedNutritionInfo

        deleteIngredientFromMealPlans(
            ingredient.id,
            persistedIngredientNutritionInfo,
            oldRecipeNutritionInfo,
            newRecipeNutritionInfo
        )

        ingredientDao.deleteIngredient(ingredient)
    }

    private suspend fun deleteIngredientFromMealPlans(
        ingredientId: Long,
        ingredientNutritionInfo: EmbeddedNutritionInfo,
        oldNutritionInfosByIngredientRecipeIds: Map<Long, EmbeddedNutritionInfo>,
        newNutritionInfosByIngredientRecipeIds: Map<Long, EmbeddedNutritionInfo>
    ) {
        val mealItemsWithIngredient = mealPlanDao.getMealItemsByIngredientId(ingredientId)
        val mealItemsWithRecipes = mealPlanDao.getMealItemsByRecipeIds(oldNutritionInfosByIngredientRecipeIds.keys.toList())

        val allMealItems = mealItemsWithIngredient + mealItemsWithRecipes
        val mealItemsByPlanId = allMealItems.groupBy { it.mealPlanId }

        val mealPlanIds = mealItemsByPlanId.keys.toList()
        val mealPlans = mealPlanDao.getMealPlansByIds(mealPlanIds)

        mealPlans.forEach { mealPlan ->
            val mealItems = mealItemsByPlanId[mealPlan.id] ?: emptyList()

            val nutritionInfoSubtractionSum = calculateNutritionSumForIngredientDelete(
                mealItems,
                ingredientNutritionInfo,
                oldNutritionInfosByIngredientRecipeIds,
                newNutritionInfosByIngredientRecipeIds
            )

            mealPlan.embeddedNutritionInfoInTotal.apply {
                calories -= nutritionInfoSubtractionSum.calories
                carbohydratesInGrams -= nutritionInfoSubtractionSum.carbohydratesInGrams
                fatInGrams -= nutritionInfoSubtractionSum.fatInGrams
                proteinInGrams -= nutritionInfoSubtractionSum.proteinInGrams
                price -= nutritionInfoSubtractionSum.price
            }
        }

        mealPlanDao.deleteMealItems(mealItemsWithIngredient)
        mealPlanDao.updateMealPlans(mealPlans)
    }

    private fun calculateNutritionSumForIngredientDelete(
        mealItems: List<MealItem>,
        ingredientNutritionInfo: EmbeddedNutritionInfo,
        oldNutritionInfosByIngredientRecipeIds: Map<Long, EmbeddedNutritionInfo>,
        newNutritionInfosByIngredientRecipeIds: Map<Long, EmbeddedNutritionInfo>
    ): EmbeddedNutritionInfo {
        var totalCalories = 0f
        var totalCarbohydrates = 0f
        var totalFat = 0f
        var totalProtein = 0f
        var totalPrice = 0f

        for (mealItem in mealItems) {
            val quantity = mealItem.quantity
            when (mealItem.mealItemType) {
                // For INGREDIENT type, we add the entire nutrition values of the ingredient
                MealItemType.INGREDIENT -> {
                    totalCalories += ingredientNutritionInfo.calories * quantity
                    totalCarbohydrates += ingredientNutritionInfo.carbohydratesInGrams * quantity
                    totalFat += ingredientNutritionInfo.fatInGrams * quantity
                    totalProtein += ingredientNutritionInfo.proteinInGrams * quantity
                    totalPrice += ingredientNutritionInfo.price * quantity
                }
                // For RECIPE type, add the difference between old and new recipe nutrition values
                MealItemType.RECIPE -> {
                    val oldRecipeNutritionInfo = oldNutritionInfosByIngredientRecipeIds[mealItem.mealItemId]
                    val newRecipeNutritionInfo = newNutritionInfosByIngredientRecipeIds[mealItem.mealItemId]

                    if (oldRecipeNutritionInfo != null && newRecipeNutritionInfo != null) {
                        totalCalories += (oldRecipeNutritionInfo.calories - newRecipeNutritionInfo.calories) * quantity
                        totalCarbohydrates += (oldRecipeNutritionInfo.carbohydratesInGrams - newRecipeNutritionInfo.carbohydratesInGrams) * quantity
                        totalFat += (oldRecipeNutritionInfo.fatInGrams - newRecipeNutritionInfo.fatInGrams) * quantity
                        totalProtein += (oldRecipeNutritionInfo.proteinInGrams - newRecipeNutritionInfo.proteinInGrams) * quantity
                        totalPrice += (oldRecipeNutritionInfo.price - newRecipeNutritionInfo.price) * quantity
                    }
                }
            }
        }

        return EmbeddedNutritionInfo(
            calories = totalCalories,
            carbohydratesInGrams = totalCarbohydrates,
            fatInGrams = totalFat,
            proteinInGrams = totalProtein,
            price = totalPrice
        )
    }

    private fun subtractIngredientFromRecipeInfo(recipe: Recipe, ingredientInfo: EmbeddedNutritionInfo, quantity: Int) {
        recipe.embeddedNutritionInfoInTotal.apply {
            calories -= ingredientInfo.calories * quantity
            price -= ingredientInfo.price * quantity
            carbohydratesInGrams -= ingredientInfo.carbohydratesInGrams * quantity
            fatInGrams -= ingredientInfo.fatInGrams * quantity
            proteinInGrams -= ingredientInfo.proteinInGrams * quantity
        }
    }

    fun getAllIngredients() = ingredientDao.getAllIngredients()

    fun getAllIngredientsNotInParam(ingredientIds: List<Long>) = ingredientDao.getIngredientsNotInList(ingredientIds)

    fun getIngredientById(ingredientId: Long): LiveData<Ingredient> {
        // quick fix to avoid ClassCastException in VM
        val ingredientLiveData = MediatorLiveData<Ingredient>()
        ingredientLiveData.addSource(ingredientDao.getIngredientLiveDataById(ingredientId)) { ingredient ->
            ingredientLiveData.value = ingredient
        }
        return ingredientLiveData
    }

    @Transaction
    suspend fun insertRecipeWithIngredients(recipeWithIngredients: RecipeWithRecipeIngredients): LiveData<RecipeWithRecipeIngredients> {
        val recipe = recipeWithIngredients.recipe
        val insertedRecipeId = recipeDao.insertRecipe(recipe)

        val recipeIngredientRefs = mutableListOf<RecipeIngredientRef>()
        for (recipeIngredient in recipeWithIngredients.ingredients) {
            recipeIngredientRefs.add(
                RecipeIngredientRef(
                    insertedRecipeId,
                    recipeIngredient.ingredient.id,
                    recipeIngredient.quantity,
                    recipeIngredient.sequenceIndex
                )
            )
        }
        recipeDao.insertRecipeIngredientRefs(recipeIngredientRefs.toList())

        return getRecipeWithIngredients(insertedRecipeId)
    }

    @Transaction
    suspend fun updateRecipeWithIngredients(recipeWithIngredients: RecipeWithRecipeIngredients) {
        // update its ingredients
        val newRecipeIngredients = mutableListOf<RecipeIngredientRef>()
        val updatedRecipeIngredients = mutableListOf<RecipeIngredientRef>()
        val deletedRecipeIngredients = mutableListOf<RecipeIngredientRef>()
        val updatedRecipe = recipeWithIngredients.recipe

        for (recipeIngredient in recipeWithIngredients.ingredients) {
            val recipeIngredientChangeStatus = recipeIngredient.changeStatus
            val recipeIngredientCrossRef = RecipeIngredientRef(
                updatedRecipe.id,
                recipeIngredient.ingredient.id,
                recipeIngredient.quantity,
                recipeIngredient.sequenceIndex
            )
            when (recipeIngredientChangeStatus) {
                ChangeStatus.NEW -> newRecipeIngredients.add(recipeIngredientCrossRef)
                ChangeStatus.CHANGED -> updatedRecipeIngredients.add(recipeIngredientCrossRef)
                ChangeStatus.DELETED -> deletedRecipeIngredients.add(recipeIngredientCrossRef)
                else -> Unit
            }
        }

        // delete before insert for re-entered ingredients during same session
        if (deletedRecipeIngredients.isNotEmpty()) {
            recipeDao.deleteRecipeIngredientRefs(deletedRecipeIngredients)
        }
        if (newRecipeIngredients.isNotEmpty()) {
            recipeDao.insertRecipeIngredientRefs(newRecipeIngredients)
        }
        if (updatedRecipeIngredients.isNotEmpty()) {
            recipeDao.updateRecipeIngredientRefs(updatedRecipeIngredients)
        }

        val oldNutritionInfo = recipeDao.getRecipeById(updatedRecipe.id).embeddedNutritionInfoInTotal
        val newNutritionInfo = updatedRecipe.embeddedNutritionInfoInTotal

        recipeDao.updateRecipe(updatedRecipe)

        if (isNutritionInfoChanged(oldNutritionInfo, newNutritionInfo)) {
            reflectUpdateRecipeInMealPlans(updatedRecipe.id, oldNutritionInfo, newNutritionInfo)
        }
    }

    private suspend fun reflectUpdateRecipeInMealPlans(
        recipeId: Long,
        oldNutritionInfo: EmbeddedNutritionInfo,
        newNutritionInfo: EmbeddedNutritionInfo
    ) {
        val mealItemsWithRecipe = mealPlanDao.getMealItemsByRecipeId(recipeId)
        val mealItemsByPlanId = mealItemsWithRecipe.groupBy { it.mealPlanId }
        val mealPlanIds = mealItemsByPlanId.keys.toList()
        val mealPlans = mealPlanDao.getMealPlansByIds(mealPlanIds)

        mealPlans.forEach { mealPlan ->
            val mealItems = mealItemsByPlanId[mealPlan.id] ?: emptyList()

            val oldNutritionInfoSum = calculateNutritionSumForRecipeMealItems(
                mealItems, oldNutritionInfo
            )
            val newNutritionInfoSum = calculateNutritionSumForRecipeMealItems(
                mealItems, newNutritionInfo
            )

            updateMealPlanWithNutritionDiff(mealPlan, oldNutritionInfoSum, newNutritionInfoSum)
        }

        mealPlanDao.updateMealPlans(mealPlans)
    }

    private fun calculateNutritionSumForRecipeMealItems(
        mealItems: List<MealItem>,
        recipeNutritionInfo: EmbeddedNutritionInfo
    ): EmbeddedNutritionInfo {
        var totalCalories = 0f
        var totalCarbohydrates = 0f
        var totalFat = 0f
        var totalProtein = 0f
        var totalPrice = 0f

        for (mealItem in mealItems) {
            val quantity = mealItem.quantity
            totalCalories += recipeNutritionInfo.calories * quantity
            totalCarbohydrates += recipeNutritionInfo.carbohydratesInGrams * quantity
            totalFat += recipeNutritionInfo.fatInGrams * quantity
            totalProtein += recipeNutritionInfo.proteinInGrams * quantity
            totalPrice += recipeNutritionInfo.price * quantity
        }

        return EmbeddedNutritionInfo(
            calories = totalCalories,
            carbohydratesInGrams = totalCarbohydrates,
            fatInGrams = totalFat,
            proteinInGrams = totalProtein,
            price = totalPrice
        )
    }

    @Transaction
    suspend fun deleteRecipeWithIngredients(recipeWithIngredients: RecipeWithRecipeIngredients) {
        val recipeId = recipeWithIngredients.recipe.id
        val persistedRecipeNutritionInfo = recipeDao.getRecipeById(recipeId).embeddedNutritionInfoInTotal

        recipeDao.deleteRecipeIngredientRefsByRecipeId(recipeId)

        deleteRecipeFromMealPlans(recipeId, persistedRecipeNutritionInfo)

        recipeDao.deleteRecipe(recipeWithIngredients.recipe)
    }

    private suspend fun deleteRecipeFromMealPlans(
        recipeId: Long,
        recipeNutritionInfo: EmbeddedNutritionInfo
    ) {
        val mealItemsWithRecipe = mealPlanDao.getMealItemsByRecipeId(recipeId)
        val mealItemsByPlanId = mealItemsWithRecipe.groupBy { it.mealPlanId }
        val mealPlanIds = mealItemsByPlanId.keys.toList()
        val mealPlans = mealPlanDao.getMealPlansByIds(mealPlanIds)

        mealPlans.forEach { mealPlan ->
            val mealItems = mealItemsByPlanId[mealPlan.id] ?: emptyList()

            val nutritionInfoSubtractionSum = calculateNutritionSumForRecipeMealItems(
                mealItems, recipeNutritionInfo
            )

            mealPlan.embeddedNutritionInfoInTotal.apply {
                calories -= nutritionInfoSubtractionSum.calories
                carbohydratesInGrams -= nutritionInfoSubtractionSum.carbohydratesInGrams
                fatInGrams -= nutritionInfoSubtractionSum.fatInGrams
                proteinInGrams -= nutritionInfoSubtractionSum.proteinInGrams
                price -= nutritionInfoSubtractionSum.price
            }
        }

        mealPlanDao.updateMealPlans(mealPlans)
        mealPlanDao.deleteMealItems(mealItemsWithRecipe)
    }

    fun getAllRecipes() = recipeDao.getAllRecipes()

    // todo: theoretically observe ingredients too, same as with meal plan
    fun getRecipeWithIngredients(recipeId: Long): LiveData<RecipeWithRecipeIngredients> {
        val result = MediatorLiveData<RecipeWithRecipeIngredients>()
        val recipeWithIngredientsLiveData = recipeDao.getRecipeWithIngredients(recipeId)
        val refsLiveData = recipeDao.getRecipeIngredientRefs(recipeId)

        fun combine(recipeWithIngredients: RecipeWithIngredients?, refs: List<RecipeIngredientRef>?) {
            if (recipeWithIngredients != null && refs != null) {
                val completeIngredients = refs.mapNotNull { ref ->
                    val ingredient = recipeWithIngredients.ingredients.firstOrNull { it.id == ref.ingredientId }
                    ingredient?.let {
                        RecipeIngredient(
                            ingredient = it,
                            quantity = ref.quantity, // Übernimm die Menge aus der Referenz
                            sequenceIndex = ref.sequenceIndex // Übernimm den sequenceIndex aus der Referenz
                        )
                    }
                }.toMutableList()

                completeIngredients.forEachIndexed { index, recipeIngredient ->
                    completeIngredients[index] = recipeIngredient.copy(sequenceIndex = index)
                }

                result.value = RecipeWithRecipeIngredients(
                    recipe = recipeWithIngredients.recipe,
                    ingredients = completeIngredients
                )
            }
        }

        result.addSource(recipeWithIngredientsLiveData) { recipeWithIngredients ->
            combine(recipeWithIngredients, refsLiveData.value)
        }

        result.addSource(refsLiveData) { refs ->
            combine(recipeWithIngredientsLiveData.value, refs)
        }

        return result
    }

    // meal plan
    @Transaction
    suspend fun insertMealPlanWithItems(mealPlanWithItems: MealPlanWithItems): LiveData<MealPlanWithItems> {
        val mealPlan = mealPlanWithItems.mealPlan
        val insertedMealPlanId = mealPlanDao.insertMealPlan(mealPlan)

        val mealItems = mealPlanWithItems.itemsByMealType.values.flatten().map { mealItem ->
            MealItem(
                mealPlanId = insertedMealPlanId,
                mealItemId = mealItem.mealItemId,
                quantity = mealItem.quantity,
                sequenceIndex = mealItem.sequenceIndex,
                mealType = mealItem.mealType,
                mealItemType = mealItem.mealItemType
            )
        }

        mealPlanDao.insertMealItems(mealItems)

        return getMealPlanWithItems(insertedMealPlanId)
    }

    @Transaction
    suspend fun updateMealPlanWithItems(mealPlanWithItems: MealPlanWithItems) {
        val newItems = mutableListOf<MealItem>()
        val updatedItems = mutableListOf<MealItem>()
        val deletedItems = mutableListOf<MealItem>()

        mealPlanWithItems.itemsByMealType.values.flatten().forEach { mealItemWithDetails ->
            val changeStatus = mealItemWithDetails.changeStatus
            val mealItemEntity = MealItem(
                id = mealItemWithDetails.id,
                mealPlanId = mealItemWithDetails.mealPlanId,
                mealItemId = mealItemWithDetails.mealItemId,
                quantity = mealItemWithDetails.quantity,
                sequenceIndex = mealItemWithDetails.sequenceIndex,
                mealType = mealItemWithDetails.mealType,
                mealItemType = mealItemWithDetails.mealItemType
            )

            when (changeStatus) {
                ChangeStatus.NEW -> newItems.add(mealItemEntity)
                ChangeStatus.CHANGED -> updatedItems.add(mealItemEntity)
                ChangeStatus.DELETED -> deletedItems.add(mealItemEntity)
                else -> Unit
            }
        }

        if (newItems.isNotEmpty()) {
            // to reset tmp negative IDs for auto generated IDs
            newItems.forEachIndexed { index, mealItem ->
                newItems[index] = mealItem.copy(id = 0L)
            }

            mealPlanDao.insertMealItems(newItems)
        }
        if (updatedItems.isNotEmpty()) {
            mealPlanDao.updateMealItems(updatedItems)
        }
        if (deletedItems.isNotEmpty()) {
            mealPlanDao.deleteMealItems(deletedItems)
        }

        mealPlanDao.updateMealPlan(mealPlanWithItems.mealPlan)
    }

    @Transaction
    suspend fun deleteMealPlanWithItems(mealPlanWithItems: MealPlanWithItems) {
        val mealItems = mealPlanWithItems.itemsByMealType.values.flatten().map { mealItemWithDetails ->
            MealItem(
                id = mealItemWithDetails.id,
                mealPlanId = mealItemWithDetails.mealPlanId,
                mealItemId = mealItemWithDetails.mealItemId,
                quantity = mealItemWithDetails.quantity,
                sequenceIndex = mealItemWithDetails.sequenceIndex,
                mealType = mealItemWithDetails.mealType,
                mealItemType = mealItemWithDetails.mealItemType
            )
        }
        mealPlanDao.deleteMealItems(mealItems)
        mealPlanDao.deleteMealPlan(mealPlanWithItems.mealPlan)
    }

    fun getAllMealPlans() = mealPlanDao.getAllMealPlans()

    fun getMealPlanWithItems(mealPlanId: Long): LiveData<MealPlanWithItems> {
        val result = MediatorLiveData<MealPlanWithItems>()

        // Obtain LiveData sources
        val mealPlanLiveData = mealPlanDao.getMealPlan(mealPlanId)
        val mealItemsLiveData = mealPlanDao.getMealItems(mealPlanId)

        val recipeIdsLiveData = mealItemsLiveData.switchMap { mealItems ->
            val recipeIds = mealItems.filter { it.mealItemType == MealItemType.RECIPE }
                .map { it.mealItemId }
            recipeDao.getRecipesByIds(recipeIds)
        }

        val ingredientIdsLiveData = mealItemsLiveData.switchMap { mealItems ->
            val ingredientIds = mealItems.filter { it.mealItemType == MealItemType.INGREDIENT }
                .map { it.mealItemId }
            ingredientDao.getIngredientsByIds(ingredientIds)
        }

        fun combine(mealPlan: MealPlan?, mealItems: List<MealItem>?, recipes: List<Recipe>?, ingredients: List<Ingredient>?) {
            if (mealPlan != null && mealItems != null && recipes != null && ingredients != null) {
                // Map Recipe and Ingredient lists for quick lookup
                val recipeMap = recipes.associateBy { it.id }
                val ingredientMap = ingredients.associateBy { it.id }

                // Create MealItemWithDetails list
                val itemsWithDetails = mealItems.map { mealItem ->
                    val recipe = if (mealItem.mealItemType == MealItemType.RECIPE) recipeMap[mealItem.mealItemId] else null
                    val ingredient = if (mealItem.mealItemType == MealItemType.INGREDIENT) ingredientMap[mealItem.mealItemId] else null
                    MealItemWithDetails(
                        id = mealItem.id,
                        mealPlanId = mealItem.mealPlanId,
                        mealItemId = mealItem.mealItemId,
                        quantity = mealItem.quantity,
                        sequenceIndex = mealItem.sequenceIndex,
                        mealType = mealItem.mealType,
                        mealItemType = mealItem.mealItemType,
                        recipe = recipe,
                        ingredient = ingredient
                    )
                }

                // Gruppieren nach MealType
                val allMealTypes = MealType.entries.toSet()
                val itemsByMealType = allMealTypes.associateWith { mealType ->
                    itemsWithDetails
                        .filter { it.mealType == mealType }
                        .sortedBy { it.sequenceIndex }
                }.withDefault { emptyList() }.toMutableMap()

                for (mealType in allMealTypes) {
                    val mealTypeItems = itemsByMealType[mealType]!!.toMutableList()
                    for ((index, mealItem) in mealTypeItems.withIndex()) {
                        mealTypeItems[index] = mealItem.copy(sequenceIndex = index)
                    }
                    itemsByMealType[mealType] = mealTypeItems
                }

                // Nährwerte für jede Gruppe berechnen
                val nutritionByMealType = itemsByMealType.mapValues { (_, items) ->
                    items.fold(EmbeddedNutritionInfo()) { acc, item ->
                        // Extrahiere Nährwerte aus Rezepten oder Zutaten
                        val calories = item.recipe?.embeddedNutritionInfoInTotal?.calories ?: item.ingredient?.normalisedEmbeddedNutritionInfo?.calories ?: 0f
                        val proteins = item.recipe?.embeddedNutritionInfoInTotal?.proteinInGrams ?: item.ingredient?.normalisedEmbeddedNutritionInfo?.proteinInGrams ?: 0f
                        val fats = item.recipe?.embeddedNutritionInfoInTotal?.fatInGrams ?: item.ingredient?.normalisedEmbeddedNutritionInfo?.fatInGrams ?: 0f
                        val carbs = item.recipe?.embeddedNutritionInfoInTotal?.carbohydratesInGrams ?: item.ingredient?.normalisedEmbeddedNutritionInfo?.carbohydratesInGrams ?: 0f
                        val price = item.recipe?.embeddedNutritionInfoInTotal?.price ?: item.ingredient?.normalisedEmbeddedNutritionInfo?.price ?: 0f
                        val quantity = item.quantity

                        EmbeddedNutritionInfo(
                            price = acc.price + price * quantity,
                            calories = acc.calories + calories * quantity,
                            fatInGrams = acc.fatInGrams + fats * quantity,
                            carbohydratesInGrams = acc.carbohydratesInGrams + carbs * quantity,
                            proteinInGrams = acc.proteinInGrams + proteins * quantity
                        )
                    }
                }

                result.value = MealPlanWithItems(
                    mealPlan = mealPlan,
                    itemsByMealType = itemsByMealType,
                    infoByMealType = nutritionByMealType
                )
            }
        }

        result.addSource(mealPlanLiveData) { mealPlan ->
            combine(mealPlan, mealItemsLiveData.value, recipeIdsLiveData.value, ingredientIdsLiveData.value)
        }

        result.addSource(mealItemsLiveData) { mealItems ->
            combine(mealPlanLiveData.value, mealItems, recipeIdsLiveData.value, ingredientIdsLiveData.value)
        }

        result.addSource(recipeIdsLiveData) { recipes ->
            combine(mealPlanLiveData.value, mealItemsLiveData.value, recipes, ingredientIdsLiveData.value)
        }

        result.addSource(ingredientIdsLiveData) { ingredients ->
            combine(mealPlanLiveData.value, mealItemsLiveData.value, recipeIdsLiveData.value, ingredients)
        }

        return result
    }
}