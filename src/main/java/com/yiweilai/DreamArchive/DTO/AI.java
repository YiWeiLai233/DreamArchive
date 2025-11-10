package com.yiweilai.DreamArchive.DTO;

import java.util.List;

public class AI {

    private String temperature;
    private String model;
    private List<messages> messages;

    public AI() {
    }

    public AI(String temperature, String model, List<messages> messages) {
        this.temperature = temperature;
        this.model = model;
        this.messages = messages;
    }

    /**
     * 获取
     * @return temperature
     */
    public String getTemperature() {
        return temperature;
    }

    /**
     * 设置
     * @param temperature
     */
    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    /**
     * 获取
     * @return model
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置
     * @param model
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取
     * @return messages
     */
    public List<messages> getMessages() {
        return messages;
    }

    /**
     * 设置
     * @param messages
     */
    public void setMessages(List<messages> messages) {
        this.messages = messages;
    }

    public String toString() {
        return "AI{temperature = " + temperature + ", model = " + model + ", messages = " + messages + "}";
    }
}
