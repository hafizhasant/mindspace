package com.local.chat;

import java.util.ArrayList;
import java.util.List;

final class Message {
    final boolean fromUser;
    String text;
    boolean loading;
    String image;
    final List<String[]> sources = new ArrayList<>();

    Message(boolean fromUser, String text) {
        this.fromUser = fromUser;
        this.text = text;
    }
}
