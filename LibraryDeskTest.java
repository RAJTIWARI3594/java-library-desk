import java.nio.file.*;
public class LibraryDeskTest {
 static int count=0;
 static void check(boolean x){if(!x)throw new AssertionError("check "+(count+1));count++;}
 static void rejects(Runnable r){boolean ok=false;try{r.run();}catch(IllegalArgumentException e){ok=true;}check(ok);}
 public static void main(String[] args)throws Exception{
  var d=new LibraryDesk();d.add("B1","Synthetic Java");check(d.books.size()==1);
  rejects(()->d.add("B1","again"));rejects(()->d.add("../evil","x"));rejects(()->d.add("B2",""));
  d.borrow("B1","SYN-1");check(d.get("B1").borrower().equals("SYN-1"));rejects(()->d.borrow("B1","SYN-2"));rejects(()->d.borrow("B9","SYN-2"));
  d.giveBack("B1");check(d.get("B1").borrower().isEmpty());rejects(()->d.giveBack("B1"));check(d.events.size()==3);
  Path temp=Files.createTempDirectory("library-test");Path p=temp.resolve("ledger.tsv");d.add("B2","Unicode café and =1+1");d.save(p);var fresh=LibraryDesk.load(p);check(fresh.get("B2").title().equals("Unicode café and =1+1"));check(fresh.events.equals(d.events));
  Files.writeString(p,"garbage");boolean bad=false;try{LibraryDesk.load(p);}catch(java.io.IOException e){bad=true;}check(bad);
  check(LibraryDesk.run(new String[]{temp.resolve("cli.tsv").toString(),"add","B1","CLI sample"})==0);check(LibraryDesk.run(new String[]{temp.resolve("cli.tsv").toString(),"borrow","B1","SYN-CLI"})==0);check(!LibraryDesk.load(temp.resolve("cli.tsv")).get("B1").borrower().isEmpty());
  check(LibraryDesk.run(new String[]{temp.resolve("cli.tsv").toString(),"unknown"})==2);
  System.out.println(count+" Java assertions passed");
 }
}
