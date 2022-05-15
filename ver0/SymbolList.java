package lava6.ver0;
import java.util.Iterator;
import java.util.function.Consumer;

/**
* It seems easy enough to make my own list class
*/

public class SymbolList implements Iterator<Symbol> {
		Symbol[] list;
		//ptr holds the address of the next entry to be added, also it contains the size
		int ptr=0;
		//iterator - points to next element to iterate
		int iter=0;

		//make this bigger than you think you need because it doesn't resize
		public SymbolList(int size) {
			list=new Symbol[size];
		}

		//I could just use LinkedList here, but I am trying to keep this simple
		public void add(Symbol lo) {list[ptr++]=lo; }
		public void push(Symbol lo) {list[ptr++]=lo; }

		public int size() {return ptr;}

		public void clear() {
			for (int i=0;i<ptr;i++) {
				list[i]=null;
			}
			ptr=0;
		}

		//look at the top of the stack without removing it
		public Symbol peek() {
			if (ptr>0) {return list[ptr-1];}
			else {
				//this is illegal array access, but just return 0
				return null;
			}
		}

		public Symbol pop() {
			return list[ptr--];
		}

		public Iterator iterator() {return (Iterator)this;}

		public void forEachRemaining(Consumer c) {return;}
		public void remove() {return;}

		public boolean hasNext() {
			return iter < ptr;
		}

		public Symbol next() {
			//the postfix increments after retrieval, which is what we want
			//the iter always points to the next item to be retrieved
			Symbol n = list[iter++];
			if (n == null) throw new IllegalStateException("symbol is null at iter "+iter);
			//else System.out.println("SymbolList DEBUG "+(iter-1)+": "+n.getSymbolTypeName());
			return n;
		}

		//does the same thing as next except doesn't increment the pointer
		//could cause an array access error but probably wont
		public Symbol lookahead() {
			return list[iter];
		}

	}