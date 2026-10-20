import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.time.Instant;

public class LibraryDesk {
 record Book(String id,String title,String borrower) {}
 final LinkedHashMap<String,Book> books=new LinkedHashMap<>();
 final ArrayList<String> events=new ArrayList<>();
 static String id(String s){if(s==null||!s.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}"))throw new IllegalArgumentException("ID must be 1–64 letters/digits/._-");return s;}
 static String title(String s){if(s==null||s.isBlank()||s.length()>160||s.chars().anyMatch(c->c<32))throw new IllegalArgumentException("Title must be 1–160 printable characters");return s.strip();}
 void add(String key,String text){id(key);title(text);if(books.containsKey(key))throw new IllegalArgumentException("Book already exists");books.put(key,new Book(key,text.strip(),""));event("ADD",key,"");}
 void borrow(String key,String who){id(who);Book b=get(key);if(!b.borrower.isEmpty())throw new IllegalArgumentException("Book is already checked out");books.put(key,new Book(key,b.title,who));event("BORROW",key,who);}
 void giveBack(String key){Book b=get(key);if(b.borrower.isEmpty())throw new IllegalArgumentException("Book is not checked out");books.put(key,new Book(key,b.title,""));event("RETURN",key,b.borrower);}
 Book get(String key){id(key);Book b=books.get(key);if(b==null)throw new IllegalArgumentException("Unknown book");return b;}
 void event(String kind,String key,String who){events.add(Instant.now()+" "+kind+" "+key+" "+who);}
 static String enc(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 static String dec(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
 static LibraryDesk load(Path p)throws IOException {
  LibraryDesk d=new LibraryDesk();if(!Files.exists(p))return d;
  if(Files.size(p)>10_000_000)throw new IOException("Ledger exceeds 10 MB");
  var lines=Files.readAllLines(p,StandardCharsets.UTF_8);
  if(lines.isEmpty()||!lines.get(0).equals("LIBRARY-DESK\t1"))throw new IOException("Invalid ledger header");
  try{
   for(String line:lines.subList(1,lines.size())){
    String[] f=line.split("\t",-1);
    if(f[0].equals("B")&&f.length==4){String key=id(dec(f[1]));String text=title(dec(f[2]));String who=dec(f[3]);if(!who.isEmpty())id(who);if(d.books.putIfAbsent(key,new Book(key,text,who))!=null)throw new IllegalArgumentException("Duplicate book");}
    else if(f[0].equals("E")&&f.length==2)d.events.add(dec(f[1]));else throw new IllegalArgumentException("Invalid row");
   }
  }catch(IllegalArgumentException e){throw new IOException("Invalid ledger: "+e.getMessage());}
  return d;
 }
 void save(Path path)throws IOException {
  Path p=path.toAbsolutePath();Path temp=Files.createTempFile(p.getParent(),".library-",".tmp");
  try{
   try(var out=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){
    out.write("LIBRARY-DESK\t1\n");
    for(Book b:books.values())out.write("B\t"+enc(b.id)+"\t"+enc(b.title)+"\t"+enc(b.borrower)+"\n");
    for(String e:events)out.write("E\t"+enc(e)+"\n");
   }
   Files.move(temp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
  }finally{Files.deleteIfExists(temp);}
 }
 static int run(String[] a){
  if(a.length<2){System.err.println("Usage: LibraryDesk LEDGER add ID TITLE | borrow ID BORROWER | return ID | list | history");return 2;}
  Path p=Path.of(a[0]).toAbsolutePath();String cmd=a[1];
  try(var channel=FileChannel.open(Path.of(p+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){
   LibraryDesk d=load(p);boolean changed=true;
   switch(cmd){
    case "add"->{if(a.length!=4)throw new IllegalArgumentException("add needs ID TITLE");d.add(a[2],a[3]);}
    case "borrow"->{if(a.length!=4)throw new IllegalArgumentException("borrow needs ID BORROWER");d.borrow(a[2],a[3]);}
    case "return"->{if(a.length!=3)throw new IllegalArgumentException("return needs ID");d.giveBack(a[2]);}
    case "list"->{if(a.length!=2)throw new IllegalArgumentException("list needs no extra arguments");changed=false;for(Book b:d.books.values())System.out.println(b.id+" | "+b.title+" | "+(b.borrower.isEmpty()?"AVAILABLE":"OUT: "+b.borrower));}
    case "history"->{if(a.length!=2)throw new IllegalArgumentException("history needs no extra arguments");changed=false;d.events.forEach(System.out::println);}
    default->throw new IllegalArgumentException("Unknown command");
   }
   if(changed){d.save(p);System.out.println("Saved "+cmd);}return 0;
  }catch(IOException|IllegalArgumentException e){System.err.println("Error: "+e.getMessage());return 2;}
 }
 public static void main(String[] a){System.exit(run(a));}
}
