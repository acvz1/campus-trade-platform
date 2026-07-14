package com.campus.trade.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressDTO(
        @JsonAlias("contact")
        @NotBlank(message = "联系人不能为空")
        @Size(max = 50, message = "联系人姓名过长")
        String contactName,
        @JsonAlias("phone")
        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
        String contactPhone,
        @Size(max = 100, message = "校区名称过长") String campus,
        @Size(max = 100, message = "楼栋名称过长") String building,
        @Size(max = 100, message = "房间号过长") String room,
        @Size(max = 255, message = "详细地址过长") String detail,
        Boolean isDefault
) {
    @AssertTrue(message = "请填写校区和楼栋，或填写完整详细地址")
    public boolean isLocationPresent() {
        boolean structured = campus != null && !campus.isBlank() && building != null && !building.isBlank();
        return structured || (detail != null && !detail.isBlank());
    }
}
