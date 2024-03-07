package com.reclizer.csgobox.config;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.item.ItemCsgoBox;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

public class CsgoBoxManage {

    private static final Gson GSON = new Gson();
    private static final Path CONFIG_DIR = Paths.get("config").resolve("csbox");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("box.json");

    public static List<ItemCsgoBox.BoxInfo> BOX = Lists.newArrayList();

    public static void loadConfigBox() throws IOException {
        if (!Files.isDirectory(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }

        File file = CONFIG_FILE.toFile();
        InputStream stream = null;
        if (Files.exists(CONFIG_FILE)) {
            stream = Files.newInputStream(file.toPath());
        } else {
            ResourceLocation res = new ResourceLocation(CsgoBox.MODID, "box.json");
            Optional<Resource> optional = Minecraft.getInstance().getResourceManager().getResource(res);
            if (optional.isPresent()) {
                stream = optional.get().open();
            }
        }
        if (stream != null) {
            BOX = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8),
                    new TypeToken<List<ItemCsgoBox.BoxInfo>>() {
                    }.getType());
        }
    }


    public static void updateBoxJson(String name, List<String> item,List<Integer> grade)throws IOException {
        // 读取原始的 JSON 数据
//        File file = CONFIG_FILE.toFile();
//        InputStream stream = null;
//        if (Files.exists(CONFIG_FILE)) {
//            stream = Files.newInputStream(file.toPath());
//        } else {
//            ResourceLocation res = new ResourceLocation(CsgoBox.MODID, "box.json");
//            Optional<Resource> optional = Minecraft.getInstance().getResourceManager().getResource(res);
//            if (optional.isPresent()) {
//                stream = optional.get().open();
//            }
//        }
        //JsonArray jsonArray=GSON.fromJso


        JsonArray jsonArray = readJsonFile(CONFIG_FILE);
        //"random": [2, 5, 6,20, 625],
        // 创建新的 JSON 对象
        JsonObject newObject = new JsonObject();
        newObject.addProperty("name", name);
        newObject.addProperty("key", "csgobox:csgo_key0");
        newObject.addProperty("drop", 0);

//        newObject.addProperty("random",0);
        JsonArray jsonInt=new JsonArray();
        jsonInt.add(2);
        jsonInt.add(5);
        jsonInt.add(6);
        jsonInt.add(20);
        jsonInt.add(625);
        newObject.add("random", jsonInt);

        JsonArray jsonArray0=new JsonArray();
        jsonArray0.add("minecraft:zombie");
        newObject.add("entity", jsonArray0);

        JsonArray jsonArray1=new JsonArray();
        JsonArray jsonArray2=new JsonArray();
        JsonArray jsonArray3=new JsonArray();
        JsonArray jsonArray4=new JsonArray();
        JsonArray jsonArray5=new JsonArray();
        for (int i=0;i<item.size();i++){
            switch (grade.get(i)) {
                case 1 -> jsonArray1.add(item.get(i));
                case 2 -> jsonArray2.add(item.get(i));
                case 3 -> jsonArray3.add(item.get(i));
                case 4 -> jsonArray4.add(item.get(i));
                case 5 -> jsonArray5.add(item.get(i));
            }
        }


        newObject.add("grade1", jsonArray1);
        newObject.add("grade2", jsonArray2);
        newObject.add("grade3", jsonArray3);
        newObject.add("grade4", jsonArray4);
        newObject.add("grade5", jsonArray5);

        // 将新的对象添加到数组中
        jsonArray.add(newObject);

        // 将更新后的数组写回文件
        writeJsonFile(CONFIG_FILE, jsonArray);
    }

    private static JsonArray readJsonFile(Path filePath) {
        try  {
            // 使用 JsonParser 解析 JSON 文件
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            return gson.fromJson(Files.newBufferedReader(filePath, StandardCharsets.UTF_8), JsonArray.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonArray();
    }

    private static void writeJsonFile(Path filePath, JsonArray jsonArray) {
        try  {

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonContent = gson.toJson(jsonArray);

            // 使用 Files.writeString 写入文件
            //Files.writeString(filePath, jsonContent, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(filePath, jsonContent, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }






}
