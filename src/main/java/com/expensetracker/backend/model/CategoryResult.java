package com.expensetracker.backend.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class CategoryResult {
    private String category;
    private double confidence;
    private String reasoning;
}