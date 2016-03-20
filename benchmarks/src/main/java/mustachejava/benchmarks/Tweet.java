package mustachejava.benchmarks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tweet {
  Integer in_reply_to_status_id = null;
  boolean possibly_sensitive = false;
  String in_reply_to_user_id_str = null;
  String contributors = null;
  boolean truncated = false;
  String id_str = "114176016611684353";

  static class User {
    int statuses_count = 5327;
    boolean profile_use_background_image = false;
    String time_zone = "Pacific Time (US & Canada)";
    boolean default_profile = false;
    boolean notifications = false;
    String profile_text_color = "333333";
    String name = "Richard Henry";
    String expanded_url = null;
    boolean default_profile_image = false;
    boolean following = true;
    boolean verified = false;
    boolean geo_enabled = true;
    String profile_background_image_url = "http://a0.twimg.com/images/themes/theme1/bg.png";
    int favourites_count = 98;
    String id_str = "31393";
    int utc_offset = -28800;
    String profile_link_color = "0084B4";
    String profile_image_url = "http://a3.twimg.com/profile_images/1192563998/Photo_on_2010-02-22_at_23.32_normal.jpeg";
    String description = "Husband to @chelsea. Designer at @twitter. English expat living in California.";
    boolean is_translator = false;
    String profile_background_image_url_https = "https://si0.twimg.com/images/themes/theme1/bg.png";
    String location = "San Francisco, CA";
    boolean follow_request_sent = false;
    int friends_count = 184;
    String profile_background_color = "404040";
    boolean profile_background_tile = false;
    String url = null;
    String display_url = null;
    String profile_sidebar_fill_color = "DDEEF6";
    int followers_count = 2895;
    String profile_image_url_https = "https://si0.twimg.com/profile_images/1192563998/Photo_on_2010-02-22_at_23.32_normal.jpeg";
    Object entities = new Object() {
      List urls = new ArrayList();
      List hashtags = new ArrayList();
      List user_mentions = Arrays.asList(new Object() {
        String name = "Chelsea Henry";
        String id_str = "16447200";
        List indices = Arrays.asList(11, 19);
        long id = 16447200;
        String screen_name = "chelsea";
      }, new Object() {
        String name = "Twitter";
        String id_str = "783214";
        List indices = Arrays.asList(33, 41);
        long id = 783214;
        String screen_name = "twitter";
      });
    };
    String lang = "en";
    boolean show_all_inline_media = true;
    int listed_count = 144;
    boolean contributors_enabled = false;
    String profile_sidebard_border_color = "C0DEED";
    int id = 31393;
    String created_at = "Wed Nov 29 22:40:31 +0000 2006";
    String screen_name = "richardhenry";
  }

  User user = new User();
  int retweet_count = 0;
  String in_reply_to_user_id = null;
  boolean favorited = false;
  String geo = null;
  String in_reply_to_screen_name = null;
  Object entities = new Object() {
    List urls = new ArrayList();
    List hashtags = new ArrayList();
    List user_mentions = new ArrayList();
    List media = Arrays.asList(new Object() {
      String type = "photo";
      String expanded_url = "http://twitter.com/richardhenry/status/114176016611684353/photo/1";
      String id_str = "114176016615878656";
      List indices = Arrays.asList(22, 42);
      String url = "http://t.co/wJxLOTOR";
      String media_url = "http://p.twimg.com/AZWie3BCMAAcQJj.jpg";
      String display_url = "pic.twitter.com/wJxLOTOR";
      long id = 114176016615878656l;
      String media_url_https = "https://p.twimg.com/AZWie3BCMAAcQJj.jpg";
      Object sizes = new Object() {
        Object small = new Object() {
          int h = 254;
          int w = 340;
          String resize = "fit";
        };
        Object large = new Object() {
          int h = 254;
          int w = 340;
          String resize = "fit";
        };
        Object thumb = new Object() {
          int h = 254;
          int w = 340;
          String resize = "fit";
        };
        Object medium = new Object() {
          int h = 254;
          int w = 340;
          String resize = "fit";
        };
      };
    });
  };
  String coordinates = null;
  String source = "<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter for  iPhone</a>";
  String place = null;
  boolean retweeted = false;
  long id = 114176016611684353l;
  String in_reply_to_status_id_str = null;
  String annotations = null;
  String text = "LOOK AT WHAT ARRIVED. http://t.co/wJxLOTOR";
  String created_at = "Thu Sep 15 03:17:38 +0000 2011";
}
