/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;

import org.json.*;

class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * 
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes
          boolean requestOk = true;

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          try {
            query_pairs = splitQuery(request.replace("multiply?", ""));
          } catch(Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("I am not sure what you want me to do...");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }

          if(query_pairs.size() != 2) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must supply no more or less than 2 integers.");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }

          // extract required fields from parameters
          Integer num1 = 0;
          Integer num2 = 0;
          try {
            num1 = Integer.parseInt(query_pairs.get("num1"));
            num2 = Integer.parseInt(query_pairs.get("num2"));
          } catch (Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must supply integers.");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }
          
          if(requestOk) {
            // do math
            Integer result = num1 * num2;

            // Generate response
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Result is: " + result);
          }

          // TODO: Include error handling here with a correct error code and
          // a response that makes sense

        } else if (request.contains("github?")) {
          // pulls the query from the request and runs it with GitHub's REST API
          // check out https://docs.github.com/rest/reference/
          //
          // HINT: REST is organized by nesting topics. Figure out the biggest one first,
          //     then drill down to what you care about
          // "Owner's repo is named RepoName. Example: find RepoName's contributors" translates to
          //     "/repos/OWNERNAME/REPONAME/contributors"

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();

          // check that query_pairs is parsable
          try {
            query_pairs = splitQuery(request.replace("github?", ""));
          } catch (Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("I am not sure what you want me to do...");
            response = builder.toString().getBytes();
            return response;
          }

          // check for well-formed query
          try {
            String query = query_pairs.get("query");

            int count = 0;
            for(int i = 0; i < query.length(); i++) {
              if(query.charAt(i) == '/') {
                count++;
              }
            }

            if(count != 2) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Malformed query.");
              response = builder.toString().getBytes();
              return response;
            }

          } catch(Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Malformed query.");
            response = builder.toString().getBytes();
            return response;
          }
          
          try {
            String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));
            JSONArray jsonArray = new JSONArray(json);
  
            StringBuilder responseJson = new StringBuilder();
            
            // TODO: Parse the JSON returned by your fetch and create an appropriate
            // response based on what the assignment document asks for
            for(int i = 0; i < jsonArray.length(); i++) {
              JSONObject jObj = (JSONObject) jsonArray.get(i);
              responseJson.append("ID: ");
              responseJson.append(jObj.getInt("id"));
              responseJson.append("\n");
              responseJson.append("Full Name: ");
              responseJson.append(jObj.getString("full_name"));
              responseJson.append("\n");
              JSONObject loginObj = jObj.getJSONObject("owner");
              responseJson.append("Login: ");
              responseJson.append(loginObj.getString("login"));
              responseJson.append("\n");
              responseJson.append("------\n");
            }
  
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(responseJson);
          } catch (Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Malformed json response.");
          }
                 
        } else if (request.contains("attackTheDarkness?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes
          boolean requestOk = true;

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          try {
            query_pairs = splitQuery(request.replace("attackTheDarkness?", ""));
          } catch(Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("I am not sure what you want me to do...");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }
          if(query_pairs.size() != 2) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must supply no more or less than 2 inputs.");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }

          // extract required fields from parameters
          String name = query_pairs.get("name");
          String weapon = query_pairs.get("weapon");

          if(name == null || weapon == null) {
            requestOk = false;
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must include values for parameters 'name' and 'weapon'");
            response = builder.toString().getBytes();
            return response;
          }
          
          if(requestOk) {
            Random random = new Random();
            int value = random.nextInt(20) + 1;

            String result = "";

            if(value < 10) {
              result = name + " attacks the darkness with a " + weapon + ", misses hilariously and is forever consumed.";
            } else if (value >= 10 && value < 15) {
              result = name + " attacks the darkness with a " + weapon + ", wounding it slightly. The darkness is now angry and full of vengeance.";
            } else if (value >= 15 && value < 18) {
              result = name + " attacks the darkness with a " + weapon + ", deeply wounding its pride. The darkness slinks off into itself, and thinks about its life.";
            } else if (value >= 18) {
              result = name + " attacks the darkness with a " + weapon + ", destroying it completely. There is much laughter and mountain dew.";
            }


            // Generate response
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(result);
          } 
        
        } 
        
        else if (request.contains("badGambler?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes
          boolean requestOk = true;

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          try {
            query_pairs = splitQuery(request.replace("badGambler?", ""));
          } catch(Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("I am not sure what you want me to do...");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }

          if(query_pairs.size() != 2) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must supply no more or less than 2 integers.");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }

          // extract required fields from parameters
          Integer num1 = 0;
          Integer num2 = 0;
          try {
            num1 = Integer.parseInt(query_pairs.get("num1"));
            num2 = Integer.parseInt(query_pairs.get("num2"));
          } catch (Exception e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must supply integers.");
            requestOk = false;
            response = builder.toString().getBytes();
            return response;
          }
          
          if(requestOk) {
            Random random = new Random();
            int value = random.nextInt(100) + 1;

            int diff1 = Math.abs(value - num1);
            int diff2 = Math.abs(value - num2);

            String[] results = {"The Earth explodes!", "All floors are made of discarded legos!", "The sun is now a flourescent light!"};
            String result = "You lose! " + results[random.nextInt(3)];

            System.out.println(diff1);
            System.out.println(diff2);
            System.out.println(value);

            if(diff1 <= 10 || diff2 <= 10) {
              result = "You win $50!";
            }

            // Generate response
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Result is: " + result);
          }

          // TODO: Include error handling here with a correct error code and
          // a response that makes sense
        }
        
        else {
          // if the request is not recognized at all

          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * 
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * 
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }
}
