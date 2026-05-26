package com.uc.caffeine.data

import com.uc.caffeine.data.dao.ConsumptionLogDao
import com.uc.caffeine.data.dao.DrinkPresetDao
import com.uc.caffeine.data.dao.DrinkUnitDao
import com.uc.caffeine.data.model.ConsumptionEntry
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.DrinkUnit
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

enum class ImportMode { MERGE, REPLACE }

class BackupManager(
    private val logDao: ConsumptionLogDao,
    private val presetDao: DrinkPresetDao,
    private val unitDao: DrinkUnitDao,
    private val settingsRepo: SettingsRepository,
) {
    suspend fun createBackup(settings: UserSettings): String {
        val entries = logDao.getAllEntriesOnce()
        val customPresets = presetDao.getCustomPresets()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", Instant.now().toString())

        val logArray = JSONArray()
        for (entry in entries) {
            logArray.put(JSONObject().apply {
                put("drinkName", entry.drinkName)
                put("caffeineMg", entry.caffeineMg)
                put("emoji", entry.emoji)
                put("presetItemId", entry.presetItemId)
                put("quantity", entry.quantity)
                put("unitKey", entry.unitKey)
                put("unitCaffeineMg", entry.unitCaffeineMg)
                put("imageName", entry.imageName)
                put("absorptionRate", entry.absorptionRate)
                put("startedAtMillis", entry.startedAtMillis)
                put("durationMinutes", entry.durationMinutes)
            })
        }
        root.put("consumptionLog", logArray)

        val settingsObj = JSONObject().apply {
            put("halfLifeMinutes", settings.halfLifeMinutes)
            put("sleepThresholdMg", settings.sleepThresholdMg)
            put("absorptionRateMinutes", settings.absorptionRateMinutes)
            put("sleepTimeHour", settings.sleepTimeHour)
            put("sleepTimeMinute", settings.sleepTimeMinute)
            put("themeMode", settings.themeMode.name)
            put("useDynamicColor", settings.useDynamicColor)
            put("use24HourClock", settings.use24HourClock)
            put("dateFormat", settings.dateFormat.name)
            put("timeZoneId", settings.timeZoneId)
            put("cyp1a2Genotype", settings.cyp1a2Genotype.name)
            put("ahrGenotype", settings.ahrGenotype.name)
            put("hormonalStatus", settings.hormonalStatus.name)
            val pf = settings.profileFactors
            pf.ageBucket?.let { put("profileAgeBucket", it) }
            put("profileWeightValue", pf.weightValue)
            put("profileWeightUnit", pf.weightUnit)
            pf.hasInsomnia?.let { put("profileHasInsomnia", it) }
            pf.smokingHabit?.let { put("profileSmokingHabit", it) }
            pf.heavyAlcohol?.let { put("profileHeavyAlcohol", it) }
            pf.heavyCaffeine?.let { put("profileHeavyCaffeine", it) }
            pf.liverDisease?.let { put("profileLiverDisease", it) }
            put("profileMedications", JSONArray(pf.medications.toList()))
            put("weeklySleepRotaEnabled", settings.weeklySleepRotaEnabled)
            put("weeklySleepRota", JSONObject().apply {
                settings.weeklySleepRota.forEach { (day, time) ->
                    put(day.name, "%02d:%02d".format(time.hour, time.minute))
                }
            })
        }
        root.put("settings", settingsObj)

        val customArray = JSONArray()
        for (preset in customPresets) {
            val units = unitDao.getUnitsForDrink(preset.id)
            val presetObj = JSONObject().apply {
                put("itemId", preset.itemId)
                put("name", preset.name)
                put("brand", preset.brand)
                put("description", preset.description ?: JSONObject.NULL)
                put("category", preset.category)
                put("imageName", preset.imageName)
                put("emoji", preset.emoji)
                put("absorptionRate", preset.absorptionRate)
                put("relevance", preset.relevance)
                put("defaultUnit", preset.defaultUnit)
                put("defaultCaffeineMg", preset.defaultCaffeineMg)
                val unitsArray = JSONArray()
                for (unit in units) {
                    unitsArray.put(JSONObject().apply {
                        put("unitKey", unit.unitKey)
                        put("caffeineMg", unit.caffeineMg)
                        put("milliliters", unit.milliliters ?: JSONObject.NULL)
                        put("grams", unit.grams ?: JSONObject.NULL)
                        put("isDefault", unit.isDefault)
                    })
                }
                put("units", unitsArray)
            }
            customArray.put(presetObj)
        }
        root.put("customDrinks", customArray)

        return root.toString(2)
    }

    suspend fun restoreBackup(json: String, mode: ImportMode) {
        val root = JSONObject(json)
        val version = root.optInt("version", 0)
        require(version == 1) { "Unsupported backup version: $version" }

        if (mode == ImportMode.REPLACE) {
            logDao.deleteAll()
            presetDao.deleteCustomPresets()
        }

        val existingKeys = if (mode == ImportMode.MERGE) {
            logDao.getAllEntriesOnce().map { "${it.startedAtMillis}|${it.drinkName}" }.toSet()
        } else emptySet()

        val logArray = root.optJSONArray("consumptionLog") ?: JSONArray()
        for (i in 0 until logArray.length()) {
            val obj = logArray.getJSONObject(i)
            val key = "${obj.getLong("startedAtMillis")}|${obj.getString("drinkName")}"
            if (key in existingKeys) continue
            logDao.logDrink(
                ConsumptionEntry(
                    drinkName = obj.getString("drinkName"),
                    caffeineMg = obj.getInt("caffeineMg"),
                    emoji = obj.optString("emoji", "☕"),
                    presetItemId = obj.optString("presetItemId", ""),
                    quantity = obj.optInt("quantity", 1),
                    unitKey = obj.optString("unitKey", ""),
                    unitCaffeineMg = obj.optDouble("unitCaffeineMg", 0.0),
                    imageName = obj.optString("imageName", ""),
                    absorptionRate = obj.optInt("absorptionRate", 45),
                    startedAtMillis = obj.getLong("startedAtMillis"),
                    durationMinutes = obj.optInt("durationMinutes", 10),
                )
            )
        }

        val settingsObj = root.optJSONObject("settings")
        if (settingsObj != null) {
            val pf = ProfileFactors(
                ageBucket = settingsObj.optString("profileAgeBucket").takeIf { it.isNotEmpty() },
                weightValue = settingsObj.optInt("profileWeightValue", 60),
                weightUnit = settingsObj.optString("profileWeightUnit", "Kilograms"),
                hasInsomnia = if (settingsObj.has("profileHasInsomnia")) settingsObj.getBoolean("profileHasInsomnia") else null,
                smokingHabit = settingsObj.optString("profileSmokingHabit").takeIf { it.isNotEmpty() },
                heavyAlcohol = if (settingsObj.has("profileHeavyAlcohol")) settingsObj.getBoolean("profileHeavyAlcohol") else null,
                heavyCaffeine = if (settingsObj.has("profileHeavyCaffeine")) settingsObj.getBoolean("profileHeavyCaffeine") else null,
                liverDisease = settingsObj.optString("profileLiverDisease").takeIf { it.isNotEmpty() },
                medications = settingsObj.optJSONArray("profileMedications")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }.toSet()
                } ?: emptySet(),
            )
            val imported = UserSettings(
                halfLifeMinutes = settingsObj.optInt("halfLifeMinutes", 300),
                sleepThresholdMg = settingsObj.optInt("sleepThresholdMg", 60),
                absorptionRateMinutes = settingsObj.optInt("absorptionRateMinutes", 45),
                sleepTimeHour = settingsObj.optInt("sleepTimeHour", 23),
                sleepTimeMinute = settingsObj.optInt("sleepTimeMinute", 0),
                themeMode = ThemeMode.fromStorage(settingsObj.optString("themeMode")),
                useDynamicColor = settingsObj.optBoolean("useDynamicColor", true),
                use24HourClock = settingsObj.optBoolean("use24HourClock", false),
                dateFormat = AppDateFormat.fromStorage(settingsObj.optString("dateFormat")),
                timeZoneId = settingsObj.optString("timeZoneId").ifEmpty { java.time.ZoneId.systemDefault().id },
                cyp1a2Genotype = Cyp1a2Genotype.fromStorage(settingsObj.optString("cyp1a2Genotype")),
                ahrGenotype = AhrGenotype.fromStorage(settingsObj.optString("ahrGenotype")),
                hormonalStatus = HormonalStatus.fromStorage(settingsObj.optString("hormonalStatus")),
                profileFactors = pf,
                weeklySleepRotaEnabled = settingsObj.optBoolean("weeklySleepRotaEnabled", false),
                weeklySleepRota = settingsObj.optJSONObject("weeklySleepRota")?.let { obj ->
                    buildMap {
                        obj.keys().forEach { key ->
                            val day = runCatching { DayOfWeek.valueOf(key) }.getOrNull() ?: return@forEach
                            val parts = obj.optString(key).split(":", limit = 2)
                            val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: return@forEach
                            val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return@forEach
                            put(day, LocalTime.of(hour, minute))
                        }
                    }
                } ?: emptyMap(),
            )
            settingsRepo.importSettings(imported)
        }

        val existingCustomIds = if (mode == ImportMode.MERGE) {
            presetDao.getCustomPresets().map { it.itemId }.toSet()
        } else emptySet()

        val customArray = root.optJSONArray("customDrinks") ?: JSONArray()
        for (i in 0 until customArray.length()) {
            val obj = customArray.getJSONObject(i)
            val itemId = obj.optString("itemId", "")
            if (itemId in existingCustomIds) continue
            val preset = DrinkPreset(
                itemId = itemId,
                name = obj.getString("name"),
                brand = obj.optString("brand", ""),
                description = if (obj.isNull("description")) null else obj.optString("description"),
                category = obj.optString("category", "Coffee"),
                imageName = obj.optString("imageName", ""),
                emoji = obj.optString("emoji", "☕"),
                absorptionRate = obj.optInt("absorptionRate", 45),
                relevance = obj.optInt("relevance", 0),
                defaultUnit = obj.optString("defaultUnit", "cup"),
                defaultCaffeineMg = obj.optInt("defaultCaffeineMg", 0),
                isCustom = true,
            )
            val drinkId = presetDao.insertAndGetId(preset).toInt()
            val unitsArray = obj.optJSONArray("units") ?: JSONArray()
            for (j in 0 until unitsArray.length()) {
                val u = unitsArray.getJSONObject(j)
                unitDao.insert(
                    DrinkUnit(
                        drinkId = drinkId,
                        unitKey = u.getString("unitKey"),
                        caffeineMg = u.getDouble("caffeineMg"),
                        milliliters = if (u.isNull("milliliters")) null else u.optDouble("milliliters"),
                        grams = if (u.isNull("grams")) null else u.optDouble("grams"),
                        isDefault = u.optBoolean("isDefault", false),
                    )
                )
            }
        }
    }
}
