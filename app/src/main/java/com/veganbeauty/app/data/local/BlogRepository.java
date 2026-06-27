package com.veganbeauty.app.data.local;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;

import com.veganbeauty.app.features.community.blog.BlogPost;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlogRepository {

    private final Context context;

    public BlogRepository(Context context) {
        this.context = context;
    }

    public List<BlogPost> getBlogPosts(int limit, String targetCategory, int offset) {
        List<BlogPost> result = new ArrayList<>();
        try {
            InputStream stream = context.getAssets().open("community_blog.json");
            JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
            reader.beginArray();
            int count = 0;
            while (reader.hasNext() && result.size() < limit) {
                String title = "";
                String description = "";
                String date = "";
                String imageUrl = "";
                String category = "";
                String doctorName = "";
                String doctorAvatar = "";
                String doctorBio = "";
                String content = "";

                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "title":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                title = reader.nextString();
                            }
                            break;
                        case "shortDescription":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                description = reader.nextString();
                            }
                            break;
                        case "publishedAt":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                String rawDate = reader.nextString();
                                try {
                                    SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                    Date d = sdfIn.parse(rawDate);
                                    SimpleDateFormat sdfOut = new SimpleDateFormat("d/M/yyyy", new Locale("vi"));
                                    if (d != null) {
                                        date = sdfOut.format(d);
                                    }
                                } catch (Exception e) {
                                    date = rawDate;
                                }
                            }
                            break;
                        case "primaryImage":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    if ("url".equals(reader.nextName())) {
                                        imageUrl = reader.nextString();
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                            }
                            break;
                        case "category":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    if ("name".equals(reader.nextName())) {
                                        category = reader.nextString();
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                            }
                            break;
                        case "approver":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    String appName = reader.nextName();
                                    if ("fullName".equals(appName)) {
                                        if (reader.peek() != JsonToken.NULL) doctorName = reader.nextString(); else reader.nextNull();
                                    } else if ("bio".equals(appName)) {
                                        if (reader.peek() != JsonToken.NULL) doctorBio = reader.nextString(); else reader.nextNull();
                                    } else if ("avatar".equals(appName)) {
                                        if (reader.peek() == JsonToken.NULL) {
                                            reader.nextNull();
                                        } else {
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                if ("url".equals(reader.nextName())) {
                                                    doctorAvatar = reader.nextString();
                                                } else {
                                                    reader.skipValue();
                                                }
                                            }
                                            reader.endObject();
                                        }
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                            }
                            break;
                        case "author":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    String authName = reader.nextName();
                                    if ("fullName".equals(authName)) {
                                        if (reader.peek() != JsonToken.NULL) {
                                            String val = reader.nextString();
                                            if (doctorName.isEmpty()) doctorName = val;
                                        } else {
                                            reader.nextNull();
                                        }
                                    } else if ("bio".equals(authName)) {
                                        if (reader.peek() != JsonToken.NULL) {
                                            String bio = reader.nextString();
                                            if (doctorBio.isEmpty()) doctorBio = bio;
                                        } else {
                                            reader.nextNull();
                                        }
                                    } else if ("avatar".equals(authName)) {
                                        if (reader.peek() == JsonToken.NULL) {
                                            reader.nextNull();
                                        } else {
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                if ("url".equals(reader.nextName())) {
                                                    String url = reader.nextString();
                                                    if (doctorAvatar.isEmpty()) doctorAvatar = url;
                                                } else {
                                                    reader.skipValue();
                                                }
                                            }
                                            reader.endObject();
                                        }
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                            }
                            break;
                        case "descriptionHtml":
                            if (reader.peek() != JsonToken.NULL) {
                                content = reader.nextString();
                            } else {
                                reader.nextNull();
                            }
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                reader.endObject();

                if (targetCategory == null || category.equals(targetCategory)) {
                    if (count >= offset) {
                        result.add(new BlogPost(title, description, date, imageUrl, category, doctorName, doctorAvatar, doctorBio, content));
                    }
                    count++;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<BlogPost> getBlogPosts(int limit, String targetCategory) {
        return getBlogPosts(limit, targetCategory, 0);
    }

    public List<BlogPost> getBlogPosts(int limit) {
        return getBlogPosts(limit, null, 0);
    }

    public List<BlogPost> getBlogPosts() {
        return getBlogPosts(10, null, 0);
    }

    public Map<String, Integer> getCategoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        try {
            InputStream stream = context.getAssets().open("community_blog.json");
            JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                while (reader.hasNext()) {
                    if ("category".equals(reader.nextName())) {
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                        } else {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                if ("name".equals(reader.nextName())) {
                                    String name = reader.nextString();
                                    counts.put(name, counts.getOrDefault(name, 0) + 1);
                                } else {
                                    reader.skipValue();
                                }
                            }
                            reader.endObject();
                        }
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counts;
    }
}
