package com.fusi24.pangreksa.web.model.view;

import com.fusi24.pangreksa.web.model.entity.HrLeaveBalance;

public class LeaveBalanceView {

    private final double allocatedDays;
    private final double usedDays;
    private final double remainingDays;

    public LeaveBalanceView(HrLeaveBalance e) {
        this.allocatedDays = e.getAllocatedDays() / 2.0;
        this.usedDays = e.getUsedDays() / 2.0;
        this.remainingDays = e.getRemainingDays() / 2.0;
    }

    public double getAllocatedDays() {
        return allocatedDays;
    }

    public double getUsedDays() {
        return usedDays;
    }

    public double getRemainingDays() {
        return remainingDays;
    }
}
