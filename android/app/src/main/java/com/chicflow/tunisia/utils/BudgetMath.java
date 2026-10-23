package com.chicflow.tunisia.utils;

/** The one formula for "percentage of the budget used" (used by Budget and Home). */
public final class BudgetMath {

    private BudgetMath() { }

    /** 180 spent of a 300 budget -> 60. Returns 0 when there is no usable budget. */
    public static int percent(double spent, double budget) {
        if (budget <= 0) return 0;
        return (int) Math.round(spent / budget * 100.0);
    }
}
