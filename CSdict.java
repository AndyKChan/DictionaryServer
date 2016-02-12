import java.lang.System;
import java.lang.Integer;
import java.io.*;
import java.net.*;
import java.util.*;

// import ca.cpsc317.dict.Command;

//
// This is an implementation of a simplified version of a command
// line dictionary client. The program takes no arguments.
//

public class CSdict {
  static final int MAX_LEN = 255;
  static final int PERMITTED_ARGUMENT_COUNT = 1;
  static final int DEFAULT_PORT = 2628;
  static final String DEFAULT_DICTIONARY = "*";
  static Boolean debugOn = false;
  static int port = 2628;
  static Boolean connected = false;
  static Socket socket = null;
  static BufferedReader in;
  static String dictionary = "*";

  public static void main(String [] args)
  {
    byte cmdString[] = new byte[MAX_LEN];

    if (args.length == PERMITTED_ARGUMENT_COUNT) {
      debugOn = args[0].equals("-d");
      if (debugOn) {
        System.out.println("Debugging output enabled");
      } else {
        errorMsgPrinter(997, "", 0);
        return;
      }
    } else if (args.length > PERMITTED_ARGUMENT_COUNT) {
      errorMsgPrinter(996, "", 0);
      return;
    }

    try {
      for (int len = 1; len > 0;) {
        System.out.print("csdict> ");
        cmdString = new byte[MAX_LEN];
        System.in.read(cmdString);
        String commandInput = new String(cmdString, "UTF-8");
        // cut off whitespace before and after string
        commandInput = commandInput.trim();

        if (commandInput.length() == 0 || commandInput.substring(0, 1).equals("#"))
          continue;
        // splits command string and puts it into commandParts string array
        String[] commandParts = commandInput.split("\\s+");

        // The case of the command is to to be ignored
        String command = commandParts[0].toLowerCase();

      	if (socket == null || socket.isClosed() == true) {
      		commandSwitch(command, commandParts, false);
      	} else if (socket.isConnected()){
      		commandSwitch(command, commandParts, true);
      	}
      }
    } catch (IOException exception) {
      System.err.println("998 Input error while reading commands, terminating.");

    } catch (NullPointerException e){
      errorMsgPrinter(925,"",0);
      socket = null;
      //  System.out.println("");
      //  String[] nextCMD = new String[1];
      // main(nextCMD);
    }
  }

  // switch for commands
  public static void commandSwitch(String command, String[] arguments, boolean socketOn) {
    if (socketOn) {
      switch (command) {
        case "open":
          errorMsgPrinter(903, "", 0);
          break;
        case "dict":
        	if(arguments.length == 1){
        		dictCMD(command);
        	}
        	else
        		errorMsgPrinter(901, "", 0);
          break;
        case "set":
          if (arguments.length == 2)
            setDictionary(arguments[1]);
          else
            errorMsgPrinter(901, "", 0);
          break;
        case "currdict":
          if (arguments.length == 1)
            currDict();
          else
            errorMsgPrinter(901, "", 0);
          break;
        case "define":
          if (arguments.length == 2)
            defineCMD(arguments[1]);
          else
            errorMsgPrinter(901, "", 0);
          break;
        case "match":
          if (arguments.length == 2)
            matchCMD(arguments[1]);
          else
            errorMsgPrinter(901, "", 0);
          break;
        case "prefixmatch":
          if (arguments.length == 2)
            prefixmatchCMD(arguments[1]);
          else
            errorMsgPrinter(901, "", 0);
          break;
        case "close":
          if (arguments.length == 1)
            closeCMD();
          else errorMsgPrinter(901, "", 0);
          break;
        case "quit":
        	if (arguments.length == 1){
        		quitCMD();
        	}
        	else errorMsgPrinter(901, "", 0);
        	break;
        default:
        	errorMsgPrinter(900, "", 0);
        	break;
      }
    } else {
      switch (command) {
        case "open":
          openCMD(arguments);
          break;
        case "dict":
        case "set":
        case "currdict":
        case "define":
        case "match":
        case "prefixmatch":
        case "close":
          errorMsgPrinter(903, "", 0);
          break;
        case "quit":
          quitCMD();
          break;
        default:
        	errorMsgPrinter(900, "", 0);
        	break;
      }
    }
  }

  // error switch
  public static void errorMsgPrinter(int errorNum, String hostName, int port) {
    String errorMsg;
    switch (errorNum) {
      case 900:
        errorMsg = "900 Invalid command.";
        break;
      case 901:
        errorMsg = "901 Incorrect number of arguments.";
        break;
      case 902:
        errorMsg = "902 Invalid argument.";
        break;
      case 903:
        errorMsg = "903 Supplied command not expected at this time.";
        break;
      case 920:
        errorMsg = "920 Control connection to " + hostName + " on port " + port + " failed to open.";
        break;
      case 925:
        errorMsg = "925 Control connection I/O error, closing control connection.";
        // close the socket when 925 is thrown
        if (socket.isConnected()) {
          try {
            socket.close();
          } catch (IOException e) {
            socket = null; // Force destroy the socket
          }
        }
        break;
      case 930:
        errorMsg = "930 Dictionary does not exist.";
        break;
      case 996:
        errorMsg = "996 Too many command line options - Only -d is allowed.";
        break;
      case 997:
        errorMsg = "997 Invalid command line option - Only -d is allowed.";
        break;
      case 998:
        errorMsg = "998 Input error while reading commands, terminating.";
        break;
      default:
        errorMsg = "999 Processing error. " + hostName + ".";
        break;
    }
    System.err.println(errorMsg);
    return;
  }

  public static void openCMD(String[] arguments) {
    String hostName = "";
    if (arguments.length == 2){
      hostName = arguments[1];
      port = DEFAULT_PORT;
    }
    else if (arguments.length == 3) {
      hostName = arguments[1];
      try {
        port = Integer.parseInt(arguments[2]);
        if (port < 0 || port > 65535) {
          errorMsgPrinter(902, "", 0);
          return;
        }
      } catch (NumberFormatException e) {
        errorMsgPrinter(902, "", 0);
        return;
      }
    }
    else {
      errorMsgPrinter(901, "", 0);
      return;
    }

    try {
      socket = new Socket();
      socket.connect(new InetSocketAddress(InetAddress.getByName(hostName), port), 30000);

      try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String openResponse = in.readLine();

        if (debugOn)
          System.out.println("<-- " + openResponse);
      } catch (IOException e) {
        errorMsgPrinter(925, "", 0);
        return;
      }

      // Set default dictionary
      dictionary = "*";
    } catch (IOException e) {
      errorMsgPrinter(920, hostName, port);
      return;
    } catch (IllegalArgumentException e) {
      errorMsgPrinter(902, "", 0);
      return;
    }
  }

  public static void closeCMD() {

    try {
      if (socket != null && socket.isConnected()){
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("QUIT");

        if (debugOn)
          System.out.println("--> QUIT");

        String openResponse = in.readLine();

        if (debugOn)
          System.out.println("<-- " + openResponse);

        socket.close();
      } else {
        errorMsgPrinter(903, "", 0);
      }
    } catch (IOException e) {
      errorMsgPrinter(925, "", 0);
      return;
    }

  }

  public static void quitCMD() {
    if (socket != null && socket.isConnected()) {
      try {
        socket.close();
      } catch (IOException e) {
        errorMsgPrinter(925, "", 0);
        return;
      }
    }
    System.exit(0);
  }

  public static void dictCMD(String command){
	  try {
			  PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			  out.println("SHOW DB");
			  Boolean end = true;

			  while (end) {
				  String lineRead = in.readLine();

          if (lineRead.length() >= 3) {
            if (lineRead.substring(0, 3).equals("110")) {
              if (debugOn)
                System.out.println("<-- " + lineRead);
              continue;
            } else if (lineRead.substring(0, 3).equals("554")) {
              if (debugOn)
                System.out.println("<-- " + lineRead);
              errorMsgPrinter(999, "No dictionaries present", 0);
              return;
            } else if (lineRead.substring(0, 3).equals("250")) {
              if (debugOn)
                System.out.println("<-- " + lineRead);
              return;
            }
          }

				  System.out.println(lineRead);
				  lineRead = lineRead.trim();
				  String[] linesRead = lineRead.split("\\s");
			  }

	  } catch (IOException exception){
		  errorMsgPrinter(920,socket.getLocalAddress().toString(),socket.getLocalPort());
		  return;
	  }
  }

  /*
    Command: set DICTIONARY
      - default is '*'
      - requires socket is connected

    Set the dictionary to retrieve definitions or matches from. The string
    representing the dictionary name can be anything. However for subsequent
    define and match commands to work the string will have to be eiher the first
    word on one of the lines returned by the dict command or one of the required
    virtual databases defined in section 3.4 of the RFC. The default dictionary
    to use if the set command has not been given is "*".

    When a connection is established to a dictionary server, the dictionary to use
    is initially set to "*". Multiple set commands simply result in a new
    dictionary to search being set. Multiple set commands do not result in the
    building of a collection of dictionaries to search.

    Note your program is not to check/ verify if the string provided as a
    dictionary name is, in fact, valid. If the name is invalid, define and match
    commands will return an error and the program will report the problem at that
    time.
  */
  public static void setDictionary(String mDictionary) {
    dictionary = mDictionary;
  }

  /*
    Prints the name of the current dictionary being used.
    Initially this value is "*".
  */
  public static void currDict() {
    System.out.println(dictionary);
  }

  public static void prefixmatchCMD(String word) {
    try {
      match(word, "prefix", dictionary);
    } catch (NoMatchException e) {
      System.out.println("*****No prefix matches found*****");
    }
  }

  public static void matchCMD(String word) {
    try {
      match(word, "exact", dictionary);
    } catch (NoMatchException e) {
      System.out.println("****No matching word(s) found****");
    }
  }

  public static void defineCMD(String word) {
    try {
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      String serverCommand = "DEFINE " + dictionary + " " + word;

      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out.println(serverCommand);
      if (debugOn)
        System.out.println("--> " + serverCommand);

      try {
        readDefinitions(in);
      } catch (NoMatchException e) {
        // if no match is found
        try {
          match(word, ".", "*");
        } catch (NoMatchException err) {
          System.out.println("***No dictionaries have a definition for this word***");
        }
      }
    } catch (IOException e) {
      errorMsgPrinter(925, "", 0);
    }
  }

  public static void match(String word, String strategy, String dictToUse) throws NoMatchException {
    try {
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      String serverCommand = "MATCH " + dictToUse + " " + strategy + " " + word;

      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out.println(serverCommand);
      if (debugOn)
        System.out.println("--> " + serverCommand);

      readMultiLines(in);

    } catch (IOException e) {
      errorMsgPrinter(925, "", 0);
      return;
    }
  }

  public static void readDefinitions(BufferedReader in)
    throws IOException, NoMatchException {
    boolean success = false;

    // Read 1 line every iteration
    while (true) {
      String line = in.readLine();

      if (!success) {
        if (debugOn)
          System.out.println("<-- " + line);
        if (line.length() >= 3) {
          // if no matches
          if (line.substring(0, 3).equals("552"))
            throw new NoMatchException();

          // if dictionary does not exists
          if (line.substring(0, 3).equals("550")) {
            errorMsgPrinter(930, "", 0);
            return;
          }

          // if success
          if (line.substring(0, 3).equals("150")) {
            success = true;
            continue;
          }
        }
      } else {
        if (line.length() >= 3) {
          if (line.substring(0, 3).equals("151")) {
            if (debugOn)
              System.out.println("<-- " + line);

            String[] parts = line.split("\\s");

            // join dictionary descriptions
            String dictDescription = "";
            for (int i = 2; i < parts.length; i++)
              dictDescription += " " + parts[i];

            System.out.println("@" + dictDescription);
          // status OK
          } else if (line.substring(0, 3).equals("250")) {
            if (debugOn)
              System.out.println("<-- " + line);
            return;
          } else {
            System.out.println(line);
          }
        } else {
          if (line.trim().length() > 0)
            System.out.println(line);
        }
      }
    }
  }

  public static void readMultiLines(BufferedReader in)
                  throws IOException, NoMatchException {
    boolean success = false;

    while (true) {
      String line = in.readLine();

      if (!success) {
        if (debugOn)
          System.out.println("<-- " + line);
        if (line.length() >= 3) {
          // if no matches
          if (line.substring(0, 3).equals("552"))
            throw new NoMatchException();
          // if dictionary does not exists
          if (line.substring(0, 3).equals("550")) {
            errorMsgPrinter(930, "", 0);
            return;
          }
          // if success
          if (line.substring(0, 3).equals("152")) {
            success = true;
            continue;
          }
        }
      }

      // detect end of response
      if (line.length() >= 3 && line.substring(0, 3).equals("250")) {
        if (debugOn)
          System.out.println("<-- " + line);
        return;
      }

      System.out.println(line);
    }
  }

  private static class NoMatchException extends Exception {

  }
  //
  // private static class DictSocket {
  //   private static int DEFAULT_PORT = 2628;
  //   private static String DEFAULT_DICTIONARY = "*";
  //
  //   private Socket socket;
  //   private String dictionary;
  //
  //   public DictSocket(String host, String port) {
  //
  //   }
  //
  //   public void connect(int timeout) {
  //
  //   }
  // }
}
