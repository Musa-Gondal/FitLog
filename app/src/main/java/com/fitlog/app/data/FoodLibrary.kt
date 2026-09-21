package com.fitlog.app.data

/**
 * Built-in food list. Values are per ONE serving and are typical averages
 * (home-cooked Pakistani portions where relevant). Real values vary with
 * recipe and oil used – add your own versions as Custom Foods for accuracy.
 */
data class FoodItem(
    val name: String,
    val serving: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val category: String,
)

object FoodLibrary {
    private fun f(name: String, serving: String, kcal: Int, p: Double, c: Double, fat: Double, cat: String) =
        FoodItem(name, serving, kcal.toDouble(), p, c, fat, cat)

    private const val BREAD = "Breads & rice"
    private const val CURRY = "Curries & daal"
    private const val MEAT = "Meat, fish & eggs"
    private const val DAIRY = "Dairy"
    private const val FRUIT = "Fruits"
    private const val VEG = "Vegetables & salad"
    private const val SNACK = "Snacks & street food"
    private const val SWEET = "Sweets & desserts"
    private const val DRINK = "Drinks"
    private const val NUTS = "Nuts & seeds"
    private const val FAST = "Fast food"
    private const val GYM = "Gym staples & supplements"
    private const val BFAST = "Breakfast items"

    val items: List<FoodItem> = listOf(
        // Breads & rice
        f("Roti / chapati (whole wheat)", "1 medium, 40 g", 120, 3.5, 22.0, 2.0, BREAD),
        f("Tandoori roti", "1 piece, 90 g", 250, 8.0, 50.0, 1.5, BREAD),
        f("Naan (plain)", "1 piece, 90 g", 260, 8.5, 45.0, 5.0, BREAD),
        f("Roghni naan", "1 piece, 110 g", 340, 9.0, 52.0, 10.0, BREAD),
        f("Paratha (plain, ghee)", "1 medium, 80 g", 260, 5.0, 32.0, 12.0, BREAD),
        f("Aloo paratha", "1 piece, 130 g", 320, 6.5, 42.0, 14.0, BREAD),
        f("Puri", "1 piece, 25 g", 100, 1.5, 11.0, 5.5, BREAD),
        f("Kulcha", "1 piece, 90 g", 270, 7.5, 46.0, 6.0, BREAD),
        f("White rice (boiled)", "1 cup, 160 g", 205, 4.3, 45.0, 0.4, BREAD),
        f("Brown rice (boiled)", "1 cup, 195 g", 215, 5.0, 45.0, 1.8, BREAD),
        f("Chicken biryani", "1 plate, 350 g", 600, 28.0, 70.0, 22.0, BREAD),
        f("Beef biryani", "1 plate, 350 g", 650, 27.0, 70.0, 27.0, BREAD),
        f("Chicken pulao", "1 plate, 300 g", 480, 24.0, 60.0, 15.0, BREAD),
        f("Matar pulao", "1 plate, 250 g", 360, 8.0, 60.0, 9.0, BREAD),
        f("Khichdi", "1 bowl, 250 g", 300, 10.0, 50.0, 6.5, BREAD),
        f("Bread slice (white)", "1 slice, 28 g", 75, 2.5, 14.0, 1.0, BREAD),
        f("Bread slice (brown / whole wheat)", "1 slice, 30 g", 75, 3.5, 12.5, 1.0, BREAD),
        f("Bun / burger bun", "1 bun, 50 g", 140, 4.5, 26.0, 2.0, BREAD),

        // Curries & daal (1 serving ≈ 1 katori / 200 g)
        f("Daal chana", "1 katori, 200 g", 250, 12.0, 32.0, 8.0, CURRY),
        f("Daal mash (urad)", "1 katori, 200 g", 270, 13.0, 28.0, 11.0, CURRY),
        f("Daal masoor", "1 katori, 200 g", 220, 12.0, 28.0, 7.0, CURRY),
        f("Daal moong", "1 katori, 200 g", 210, 12.0, 27.0, 6.0, CURRY),
        f("Chana (chickpea curry / cholay)", "1 katori, 200 g", 290, 12.0, 38.0, 10.0, CURRY),
        f("Lobia (red beans curry)", "1 katori, 200 g", 260, 12.0, 34.0, 8.0, CURRY),
        f("Chicken karahi", "1 serving, 200 g", 380, 32.0, 6.0, 25.0, CURRY),
        f("Chicken qorma", "1 serving, 200 g", 400, 28.0, 10.0, 28.0, CURRY),
        f("Chicken salan (home style)", "1 serving, 200 g", 300, 26.0, 8.0, 18.0, CURRY),
        f("Chicken handi", "1 serving, 200 g", 420, 28.0, 8.0, 30.0, CURRY),
        f("Mutton karahi", "1 serving, 200 g", 480, 30.0, 6.0, 37.0, CURRY),
        f("Beef nihari", "1 serving, 250 g", 520, 32.0, 12.0, 38.0, CURRY),
        f("Haleem", "1 bowl, 250 g", 400, 24.0, 35.0, 18.0, CURRY),
        f("Aloo gosht", "1 serving, 250 g", 420, 25.0, 22.0, 26.0, CURRY),
        f("Keema (beef mince)", "1 serving, 200 g", 400, 30.0, 8.0, 28.0, CURRY),
        f("Aloo keema", "1 serving, 200 g", 360, 22.0, 18.0, 22.0, CURRY),
        f("Palak paneer", "1 serving, 200 g", 320, 14.0, 10.0, 25.0, CURRY),
        f("Saag (sarson)", "1 serving, 200 g", 200, 6.0, 12.0, 14.0, CURRY),
        f("Bhindi (okra) masala", "1 serving, 150 g", 170, 3.0, 12.0, 12.0, CURRY),
        f("Aloo matar", "1 serving, 200 g", 220, 5.0, 26.0, 11.0, CURRY),
        f("Mixed sabzi", "1 serving, 200 g", 180, 4.0, 18.0, 10.0, CURRY),
        f("Kadhi pakora", "1 serving, 250 g", 300, 9.0, 22.0, 20.0, CURRY),
        f("Anda curry (2 eggs)", "1 serving, 200 g", 300, 14.0, 8.0, 23.0, CURRY),
        f("Fish curry", "1 serving, 200 g", 300, 26.0, 6.0, 19.0, CURRY),

        // Meat, fish & eggs (cooked)
        f("Chicken breast (grilled, no skin)", "100 g", 165, 31.0, 0.0, 3.6, MEAT),
        f("Chicken thigh (cooked, no skin)", "100 g", 210, 26.0, 0.0, 11.0, MEAT),
        f("Chicken tikka (leg piece)", "1 piece, 180 g", 330, 38.0, 4.0, 18.0, MEAT),
        f("Chicken boti / tikka boti", "6 pieces, 150 g", 260, 35.0, 3.0, 12.0, MEAT),
        f("Seekh kabab (beef)", "1 kabab, 60 g", 170, 11.0, 2.0, 13.0, MEAT),
        f("Chapli kabab", "1 kabab, 120 g", 380, 20.0, 8.0, 30.0, MEAT),
        f("Shami kabab", "1 kabab, 50 g", 130, 8.0, 7.0, 8.0, MEAT),
        f("Beef (lean, cooked)", "100 g", 220, 30.0, 0.0, 10.0, MEAT),
        f("Mutton (cooked)", "100 g", 290, 25.0, 0.0, 21.0, MEAT),
        f("Fish (grilled, rohu / white fish)", "100 g", 140, 24.0, 0.0, 4.5, MEAT),
        f("Fried fish (Lahori)", "150 g", 380, 30.0, 14.0, 23.0, MEAT),
        f("Tuna (canned in water)", "1 can drained, 120 g", 130, 29.0, 0.0, 1.0, MEAT),
        f("Egg (whole, boiled)", "1 large, 50 g", 78, 6.3, 0.6, 5.3, MEAT),
        f("Egg white", "1 large", 17, 3.6, 0.2, 0.1, MEAT),
        f("Fried egg", "1 egg", 95, 6.3, 0.4, 7.5, MEAT),
        f("Omelette (2 eggs, desi style)", "1 omelette", 220, 13.0, 3.0, 17.0, MEAT),

        // Breakfast
        f("Halwa puri (2 puri + halwa + cholay)", "1 plate", 900, 18.0, 110.0, 44.0, BFAST),
        f("Anda paratha", "1 piece", 380, 12.0, 34.0, 21.0, BFAST),
        f("Oats (dry)", "40 g", 150, 5.0, 27.0, 2.7, BFAST),
        f("Oats with milk (40 g oats + 250 ml milk)", "1 bowl", 310, 13.0, 39.0, 11.0, BFAST),
        f("Cornflakes", "30 g", 110, 2.0, 25.0, 0.3, BFAST),
        f("Peanut butter", "1 tbsp, 16 g", 95, 3.5, 3.5, 8.0, BFAST),
        f("Jam", "1 tbsp, 20 g", 55, 0.0, 14.0, 0.0, BFAST),
        f("Honey", "1 tbsp, 21 g", 64, 0.1, 17.0, 0.0, BFAST),
        f("Butter", "1 tsp, 5 g", 36, 0.0, 0.0, 4.1, BFAST),
        f("Desi ghee", "1 tbsp, 13 g", 115, 0.0, 0.0, 13.0, BFAST),
        f("Cooking oil", "1 tbsp, 14 g", 120, 0.0, 0.0, 14.0, BFAST),

        // Dairy
        f("Milk (whole / full cream)", "1 glass, 250 ml", 155, 8.0, 12.0, 8.0, DAIRY),
        f("Milk (low fat)", "1 glass, 250 ml", 110, 8.5, 12.5, 2.5, DAIRY),
        f("Dahi (yogurt, full fat)", "1 katori, 150 g", 95, 5.5, 7.0, 5.0, DAIRY),
        f("Greek yogurt (plain, low fat)", "150 g", 110, 15.0, 6.0, 3.0, DAIRY),
        f("Raita", "1 katori, 100 g", 60, 3.0, 5.0, 3.0, DAIRY),
        f("Paneer", "100 g", 290, 18.0, 3.5, 22.0, DAIRY),
        f("Cheddar cheese", "1 slice, 20 g", 80, 5.0, 0.3, 6.6, DAIRY),
        f("Cheese slice (processed)", "1 slice", 60, 3.5, 1.5, 4.5, DAIRY),

        // Fruits
        f("Banana", "1 medium, 118 g", 105, 1.3, 27.0, 0.4, FRUIT),
        f("Apple", "1 medium, 180 g", 95, 0.5, 25.0, 0.3, FRUIT),
        f("Mango", "1 cup, 165 g", 100, 1.4, 25.0, 0.6, FRUIT),
        f("Orange / kinnow", "1 medium, 130 g", 60, 1.2, 15.0, 0.2, FRUIT),
        f("Guava (amrood)", "1 medium, 100 g", 68, 2.6, 14.0, 1.0, FRUIT),
        f("Dates (khajoor)", "3 pieces, 24 g", 70, 0.6, 18.0, 0.1, FRUIT),
        f("Grapes", "1 cup, 150 g", 105, 1.1, 27.0, 0.2, FRUIT),
        f("Watermelon", "1 cup, 150 g", 45, 0.9, 11.5, 0.2, FRUIT),
        f("Melon (garma / sarda)", "1 cup, 170 g", 60, 1.4, 14.0, 0.3, FRUIT),
        f("Pomegranate (anar)", "1/2 fruit, 140 g", 115, 2.3, 26.0, 1.6, FRUIT),
        f("Papaya", "1 cup, 145 g", 62, 0.7, 16.0, 0.4, FRUIT),
        f("Pear (nashpati)", "1 medium, 178 g", 100, 0.6, 27.0, 0.2, FRUIT),

        // Veg & salad
        f("Kachumber salad", "1 plate, 150 g", 35, 1.5, 7.0, 0.3, VEG),
        f("Cucumber", "1 medium, 200 g", 30, 1.3, 7.0, 0.2, VEG),
        f("Boiled potato", "1 medium, 150 g", 130, 3.0, 30.0, 0.2, VEG),
        f("Sweet potato (shakarkandi), boiled", "1 medium, 150 g", 130, 2.5, 30.0, 0.2, VEG),
        f("Boiled chickpeas (chana)", "1 cup, 160 g", 270, 14.5, 45.0, 4.2, VEG),
        f("Mixed vegetables (steamed)", "1 cup, 150 g", 70, 3.5, 13.0, 0.5, VEG),
        f("Corn (boiled, bhutta)", "1 cob", 100, 3.5, 22.0, 1.5, VEG),

        // Snacks & street food
        f("Samosa (aloo)", "1 piece, 100 g", 260, 4.5, 30.0, 14.0, SNACK),
        f("Pakora", "5 pieces, 100 g", 300, 7.0, 25.0, 19.0, SNACK),
        f("Chana chaat", "1 plate, 200 g", 280, 11.0, 42.0, 7.0, SNACK),
        f("Fruit chaat", "1 bowl, 200 g", 150, 1.5, 36.0, 0.5, SNACK),
        f("Dahi bhalla", "1 plate, 200 g", 330, 11.0, 42.0, 13.0, SNACK),
        f("Gol gappay (pani puri)", "6 pieces", 220, 4.0, 34.0, 8.0, SNACK),
        f("Bun kabab", "1 piece", 380, 14.0, 40.0, 18.0, SNACK),
        f("Chicken roll / paratha roll", "1 roll", 500, 22.0, 45.0, 26.0, SNACK),
        f("Nimko / mixed namkeen", "30 g", 160, 4.0, 15.0, 10.0, SNACK),
        f("Potato chips (Lays etc.)", "1 small pack, 30 g", 160, 2.0, 15.0, 10.0, SNACK),
        f("Biscuits (tea biscuits)", "4 pieces, 30 g", 140, 2.0, 21.0, 5.5, SNACK),
        f("Rusk", "2 pieces, 30 g", 125, 3.5, 22.0, 2.5, SNACK),
        f("Popcorn (plain)", "3 cups, 24 g", 95, 3.0, 19.0, 1.1, SNACK),

        // Sweets
        f("Gulab jamun", "1 piece, 50 g", 175, 2.5, 25.0, 7.5, SWEET),
        f("Jalebi", "100 g", 380, 3.0, 60.0, 15.0, SWEET),
        f("Kheer", "1 katori, 150 g", 220, 6.0, 32.0, 7.5, SWEET),
        f("Gajar halwa", "1 katori, 100 g", 280, 4.5, 34.0, 14.0, SWEET),
        f("Ladoo (besan / motichoor)", "1 piece, 40 g", 180, 3.0, 22.0, 9.0, SWEET),
        f("Barfi", "1 piece, 30 g", 130, 2.5, 17.0, 6.0, SWEET),
        f("Ice cream (vanilla)", "1 scoop, 70 g", 140, 2.4, 16.0, 7.5, SWEET),
        f("Chocolate (milk)", "1 bar, 40 g", 215, 3.0, 24.0, 12.0, SWEET),
        f("Sugar", "1 tsp, 4 g", 16, 0.0, 4.0, 0.0, SWEET),

        // Drinks
        f("Doodh patti chai (with sugar)", "1 cup, 200 ml", 120, 4.0, 15.0, 5.0, DRINK),
        f("Chai (no sugar)", "1 cup, 200 ml", 70, 3.5, 5.5, 4.0, DRINK),
        f("Black coffee", "1 cup", 3, 0.3, 0.0, 0.0, DRINK),
        f("Coffee with milk (no sugar)", "1 cup, 240 ml", 70, 4.0, 6.0, 3.5, DRINK),
        f("Lassi (sweet)", "1 glass, 300 ml", 260, 9.0, 38.0, 8.0, DRINK),
        f("Lassi (salty)", "1 glass, 300 ml", 150, 9.0, 11.0, 8.0, DRINK),
        f("Soft drink (Coke / Pepsi)", "1 can, 330 ml", 140, 0.0, 35.0, 0.0, DRINK),
        f("Diet soft drink", "1 can, 330 ml", 1, 0.0, 0.0, 0.0, DRINK),
        f("Fresh orange juice", "1 glass, 250 ml", 110, 1.7, 26.0, 0.5, DRINK),
        f("Sugarcane juice (ganay ka ras)", "1 glass, 250 ml", 180, 0.0, 45.0, 0.0, DRINK),
        f("Mango shake", "1 glass, 300 ml", 330, 8.0, 55.0, 9.0, DRINK),
        f("Banana shake", "1 glass, 300 ml", 300, 9.0, 48.0, 8.5, DRINK),
        f("Rooh Afza with milk", "1 glass, 250 ml", 230, 8.0, 34.0, 7.0, DRINK),

        // Nuts
        f("Almonds (badam)", "10 pieces, 12 g", 70, 2.5, 2.5, 6.0, NUTS),
        f("Walnuts (akhrot)", "4 halves, 15 g", 100, 2.3, 2.0, 9.8, NUTS),
        f("Peanuts (roasted)", "30 g", 170, 7.5, 5.0, 14.5, NUTS),
        f("Cashews (kaju)", "15 pieces, 25 g", 140, 4.5, 8.0, 11.0, NUTS),
        f("Pistachios", "30 g", 160, 6.0, 8.0, 13.0, NUTS),
        f("Chia seeds", "1 tbsp, 12 g", 58, 2.0, 5.0, 3.7, NUTS),

        // Fast food
        f("Zinger burger", "1 burger", 550, 25.0, 50.0, 28.0, FAST),
        f("Beef burger (regular)", "1 burger", 500, 25.0, 40.0, 26.0, FAST),
        f("Pizza (regular crust)", "1 slice (large pizza)", 290, 12.0, 34.0, 12.0, FAST),
        f("French fries", "medium portion, 115 g", 365, 4.0, 48.0, 17.0, FAST),
        f("Fried chicken piece", "1 piece, 120 g", 320, 25.0, 11.0, 20.0, FAST),
        f("Shawarma (chicken)", "1 wrap", 450, 25.0, 45.0, 18.0, FAST),
        f("Club sandwich", "1 sandwich", 550, 28.0, 45.0, 28.0, FAST),
        f("Chicken pasta (creamy)", "1 plate, 300 g", 600, 30.0, 60.0, 26.0, FAST),
        f("Chow mein (chicken)", "1 plate, 300 g", 450, 22.0, 55.0, 15.0, FAST),

        // Gym staples
        f("Whey protein", "1 scoop, 30 g", 120, 24.0, 3.0, 1.5, GYM),
        f("Mass gainer", "1 serving, 100 g", 380, 15.0, 75.0, 2.0, GYM),
        f("Creatine monohydrate", "5 g", 0, 0.0, 0.0, 0.0, GYM),
        f("Protein bar", "1 bar, 60 g", 220, 20.0, 22.0, 7.0, GYM),
        f("Soya chunks (dry)", "50 g", 170, 26.0, 16.5, 0.3, GYM),
        f("Chicken breast (raw weight)", "100 g", 120, 22.5, 0.0, 2.6, GYM),
    )

    val categories: List<String> = items.map { it.category }.distinct()
}
