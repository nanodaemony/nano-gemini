package com.naon.grid.backend.domain.character;

import com.naon.grid.backend.repo.character.CharComparisonRepository;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterEntityMappingTest {

    @Test
    void charCharacterUsesSqlColumnNamesAndLengths() throws Exception {
        assertFalse(hasField(CharCharacter.class, "sequenceNo"));
        assertColumn(CharCharacter.class, "character", "`character`", 16, null);
        assertColumn(CharCharacter.class, "level", "hsk_level", 20, null);
        assertColumn(CharCharacter.class, "pinyin", "pinyin", 32, null);
        assertColumn(CharCharacter.class, "traditional", "traditional", 16, null);
        assertColumn(CharCharacter.class, "audioId", "audio_id", 255, null);
        assertColumn(CharCharacter.class, "radical", "radical", 16, null);
        assertColumn(CharCharacter.class, "radicalId", "radical_id", 255, null);
        assertColumn(CharCharacter.class, "componentCombination", "component_combination", 64, null);
        assertColumn(CharCharacter.class, "charDesc", "char_desc", 1024, null);
        assertColumn(CharCharacter.class, "descTranslations", "char_desc_translations", 255, "text");
        assertColumn(CharCharacter.class, "stroke", "stroke", 4096, null);
        assertColumn(CharCharacter.class, "draftContent", "draft_content", 255, "text");
    }

    @Test
    void charWordUsesSqlColumnNamesAndNoInlineExampleFields() throws Exception {
        assertColumn(CharWord.class, "level", "hsk_level", 20, null);
        assertColumn(CharWord.class, "wordOrder", "`order`", 255, null);
        assertFalse(hasField(CharWord.class, "exampleSentence"));
        assertFalse(hasField(CharWord.class, "examplePinyin"));
        assertFalse(hasField(CharWord.class, "exampleTranslations"));
        assertFalse(hasField(CharWord.class, "exampleImage"));
    }

    @Test
    void charComparisonReplacesDiscriminationTableAndRepository() throws Exception {
        Table table = CharComparison.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("char_comparison", table.name());
        assertColumn(CharComparison.class, "comparisonChar", "comparison_char", 10, null);
        assertColumn(CharComparison.class, "comparisonPinyin", "comparison_pinyin", 100, null);
        assertColumn(CharComparison.class, "comparisonCharTranslations", "comparison_char_translations", 255, "text");
        assertColumn(CharComparison.class, "comparisonDescTranslations", "comparison_desc_translations", 255, "text");
        assertColumn(CharComparison.class, "comparisonOrder", "`order`", 255, null);
        assertRepositoryDomain(CharComparisonRepository.class, CharComparison.class);
    }

    private static void assertColumn(Class<?> entityClass, String fieldName, String columnName, int length, String columnDefinition) throws Exception {
        Field field = entityClass.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column, fieldName + " should have @Column");
        assertEquals(columnName, column.name(), fieldName + " column name");
        assertEquals(length, column.length(), fieldName + " column length");
        if (columnDefinition != null) {
            assertEquals(columnDefinition, column.columnDefinition(), fieldName + " column definition");
        }
    }

    private static boolean hasField(Class<?> entityClass, String fieldName) {
        try {
            entityClass.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    private static void assertRepositoryDomain(Class<?> repositoryClass, Class<?> expectedDomainClass) {
        ParameterizedType type = (ParameterizedType) repositoryClass.getGenericInterfaces()[0];
        assertEquals(expectedDomainClass, type.getActualTypeArguments()[0]);
    }
}
