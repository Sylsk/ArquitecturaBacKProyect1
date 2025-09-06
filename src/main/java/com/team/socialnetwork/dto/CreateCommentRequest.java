package com.team.socialnetwork.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {
    @NotBlank
    @Size(max = 1000)
    private String text;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}

