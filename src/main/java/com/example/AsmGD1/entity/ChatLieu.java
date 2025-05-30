package com.example.AsmGD1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "chat_lieu")
@Data
public class ChatLieu {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten_chat_lieu")
    private String tenChatLieu;
}
