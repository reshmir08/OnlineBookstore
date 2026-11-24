package com.bookstore.app.data.seed

import com.bookstore.app.data.local.BookEntity
import com.bookstore.app.data.local.CategoryEntity

/** Realistic sample catalogue used to populate the database on first launch. */
object SeedData {

    val categories = listOf(
        CategoryEntity(1, "Fiction", "Novels, classics and storytelling", "fiction"),
        CategoryEntity(2, "Non-Fiction", "Biographies, history and big ideas", "nonfiction"),
        CategoryEntity(3, "Science & Technology", "Programming, science and engineering", "tech"),
        CategoryEntity(4, "Business & Finance", "Management, investing and strategy", "business"),
        CategoryEntity(5, "Children", "Picture books and young readers", "children"),
        CategoryEntity(6, "Self-Help", "Habits, mindset and personal growth", "selfhelp")
    )

    private const val INDIGO = 0xFF3F3DA8
    private const val NAVY = 0xFF1F3A5F
    private const val TEAL = 0xFF0F766E
    private const val EMERALD = 0xFF047857
    private const val CRIMSON = 0xFFB91C45
    private const val ROSE = 0xFFE11D74
    private const val AMBER = 0xFFD97706
    private const val ORANGE = 0xFFEA580C
    private const val PURPLE = 0xFF7C3AED
    private const val SLATE = 0xFF334155
    private const val SKY = 0xFF0284C7
    private const val LIME = 0xFF65A30D

    private fun book(
        id: Long, title: String, author: String, brand: String, cat: Long, price: Long,
        stock: Int, days: Int, pop: Int, c1: Long, c2: Long, description: String
    ) = BookEntity(id, title, author, brand, cat, description, price, stock, days, c1, c2, pop)

    val books = listOf(
        // Fiction
        book(101, "The Midnight Library", "Matt Haig", "Penguin Random House", 1, 399, 42, 3, 95, INDIGO, NAVY,
            "Between life and death there is a library, and within it, infinite books containing the lives you could have lived. A moving novel about regret, hope and choosing to live."),
        book(102, "Where the Crawdads Sing", "Delia Owens", "Penguin Random House", 1, 449, 30, 3, 90, TEAL, EMERALD,
            "A haunting mystery and a coming-of-age story set in the marshes of North Carolina, about a girl raised alone by nature."),
        book(103, "The Alchemist", "Paulo Coelho", "HarperCollins", 1, 299, 80, 2, 98, AMBER, ORANGE,
            "A shepherd boy's journey to the Egyptian pyramids in search of treasure becomes a timeless fable about following your dreams."),
        book(104, "To Kill a Mockingbird", "Harper Lee", "HarperCollins", 1, 349, 25, 3, 88, SLATE, NAVY,
            "A gripping portrait of racial injustice and childhood innocence in the American South, told through the eyes of Scout Finch."),
        book(105, "Harry Potter and the Philosopher's Stone", "J.K. Rowling", "Bloomsbury", 1, 499, 60, 2, 99, CRIMSON, PURPLE,
            "Harry discovers on his eleventh birthday that he is a wizard and begins his first year at Hogwarts School of Witchcraft and Wizardry."),
        book(106, "The Kite Runner", "Khaled Hosseini", "Bloomsbury", 1, 379, 0, 5, 84, ORANGE, CRIMSON,
            "A powerful story of friendship, betrayal and redemption set against the changing landscape of Afghanistan."),
        book(107, "Pride and Prejudice", "Jane Austen", "Oxford University Press", 1, 249, 55, 4, 80, ROSE, PURPLE,
            "Elizabeth Bennet and Mr Darcy navigate class, manners and misjudgement in Austen's sparkling comedy of love."),

        // Non-Fiction
        book(201, "Sapiens: A Brief History of Humankind", "Yuval Noah Harari", "Penguin Random House", 2, 549, 38, 3, 96, AMBER, SLATE,
            "A sweeping history of our species, from the Stone Age to the modern age, and the revolutions that shaped how we live."),
        book(202, "Educated", "Tara Westover", "Penguin Random House", 2, 429, 20, 3, 85, SKY, NAVY,
            "A memoir of a woman who grew up in a survivalist family and taught herself enough to earn a PhD from Cambridge."),
        book(203, "The Diary of a Young Girl", "Anne Frank", "Hachette India", 2, 279, 45, 4, 82, ROSE, CRIMSON,
            "The wartime diary of a young Jewish girl in hiding, a deeply human record of courage and hope."),
        book(204, "Wings of Fire", "A.P.J. Abdul Kalam", "Hachette India", 2, 199, 70, 2, 91, ORANGE, INDIGO,
            "The autobiography of India's Missile Man, charting a humble childhood to the heights of Indian science."),
        book(205, "A Brief History of Time", "Stephen Hawking", "Bloomsbury", 2, 459, 33, 3, 89, NAVY, PURPLE,
            "Hawking explains black holes, the Big Bang and the nature of time for the curious general reader."),
        book(206, "The Discovery of India", "Jawaharlal Nehru", "Oxford University Press", 2, 399, 18, 5, 70, EMERALD, AMBER,
            "A reflective account of India's history, culture and philosophy written during imprisonment in 1944."),

        // Science & Technology
        book(301, "Clean Code", "Robert C. Martin", "Pearson", 3, 2599, 26, 4, 93, SLATE, TEAL,
            "A handbook of agile software craftsmanship. Learn to write code that is readable, maintainable and professional."),
        book(302, "Designing Data-Intensive Applications", "Martin Kleppmann", "O'Reilly Media", 3, 3299, 15, 5, 94, NAVY, SKY,
            "The big ideas behind reliable, scalable and maintainable systems, from storage engines to distributed consensus."),
        book(303, "Kotlin in Action", "Dmitry Jemerov", "Manning Publications", 3, 2899, 22, 5, 78, PURPLE, INDIGO,
            "Learn Kotlin by building on what you know of Java, with a focus on practical idioms for real projects."),
        book(304, "Learning Python", "Mark Lutz", "O'Reilly Media", 3, 2999, 31, 4, 83, AMBER, EMERALD,
            "A thorough, example-driven introduction to the Python language, from fundamentals to advanced features."),
        book(305, "Introduction to Algorithms", "Thomas H. Cormen", "Pearson", 3, 3799, 12, 6, 86, CRIMSON, SLATE,
            "The definitive textbook on algorithms, covering a broad range of topics with rigour and clarity."),
        book(306, "Cosmos", "Carl Sagan", "Penguin Random House", 3, 499, 40, 3, 81, INDIGO, TEAL,
            "A celebrated journey through the universe, from the Big Bang to the human quest to understand it."),
        book(307, "The Pragmatic Programmer", "Andrew Hunt", "Pearson", 3, 2699, 0, 7, 87, TEAL, NAVY,
            "Timeless advice on becoming a better developer, covering craft, tooling and a pragmatic mindset."),

        // Business & Finance
        book(401, "The Psychology of Money", "Morgan Housel", "Jaico Publishing", 4, 349, 90, 2, 97, EMERALD, TEAL,
            "Timeless lessons on wealth, greed and happiness, and why how you behave matters more than what you know."),
        book(402, "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Manjul Publishing", 4, 299, 75, 2, 92, AMBER, CRIMSON,
            "What the rich teach their kids about money that the poor and middle class do not."),
        book(403, "Zero to One", "Peter Thiel", "Penguin Random House", 4, 399, 28, 3, 84, SKY, INDIGO,
            "Notes on startups and how to build the future by creating something new rather than copying what works."),
        book(404, "Good to Great", "Jim Collins", "HarperCollins", 4, 599, 24, 4, 79, SLATE, EMERALD,
            "Why some companies make the leap to lasting greatness and others do not, based on five years of research."),
        book(405, "The Intelligent Investor", "Benjamin Graham", "HarperCollins", 4, 649, 19, 4, 88, NAVY, AMBER,
            "The classic guide to value investing and defending your portfolio against emotional decisions."),
        book(406, "Thinking, Fast and Slow", "Daniel Kahneman", "Penguin Random House", 4, 499, 34, 3, 90, PURPLE, ROSE,
            "A tour of the two systems that drive how we think, and the biases that shape our choices."),

        // Children
        book(501, "The Very Hungry Caterpillar", "Eric Carle", "Penguin Random House", 5, 299, 65, 2, 94, LIME, AMBER,
            "A beloved picture book following a caterpillar's week of eating before it becomes a butterfly."),
        book(502, "Matilda", "Roald Dahl", "Penguin Random House", 5, 349, 48, 3, 88, ROSE, PURPLE,
            "A brilliant little girl with extraordinary powers takes on her awful parents and the terrifying headmistress."),
        book(503, "Geronimo Stilton: The Lost Treasure", "Geronimo Stilton", "Scholastic", 5, 249, 52, 3, 76, ORANGE, AMBER,
            "A fast-paced, fun-filled adventure with the Mouse Island journalist, full of puzzles and jokes."),
        book(504, "The Jungle Book", "Rudyard Kipling", "Scholastic", 5, 199, 58, 3, 80, EMERALD, LIME,
            "Mowgli, raised by wolves, learns the law of the jungle in Kipling's classic tales of friendship."),
        book(505, "Diary of a Wimpy Kid", "Jeff Kinney", "Scholastic", 5, 329, 37, 3, 90, SKY, TEAL,
            "Greg Heffley's hilarious, illustrated journal of surviving middle school."),
        book(506, "Panchatantra Stories", "Vishnu Sharma", "Hachette India", 5, 179, 80, 2, 74, AMBER, ROSE,
            "Classic Indian animal fables, each teaching a lesson in wisdom, friendship and cleverness."),

        // Self-Help
        book(601, "Atomic Habits", "James Clear", "Penguin Random House", 6, 449, 120, 2, 100, ORANGE, CRIMSON,
            "A proven framework for building good habits and breaking bad ones through tiny, consistent changes."),
        book(602, "Ikigai", "Héctor García", "Penguin Random House", 6, 299, 85, 2, 86, TEAL, EMERALD,
            "The Japanese secret to a long and happy life, and finding the purpose that gets you out of bed."),
        book(603, "The 7 Habits of Highly Effective People", "Stephen R. Covey", "Simon & Schuster", 6, 399, 44, 3, 85, NAVY, SKY,
            "Powerful lessons in personal change, built on principles of fairness, integrity and human dignity."),
        book(604, "Deep Work", "Cal Newport", "Hachette India", 6, 429, 36, 3, 87, SLATE, PURPLE,
            "Rules for focused success in a distracted world, and how to cultivate the skill of deep concentration."),
        book(605, "The Power of Now", "Eckhart Tolle", "Hachette India", 6, 379, 29, 4, 82, AMBER, TEAL,
            "A guide to spiritual enlightenment that shows how to live fully in the present moment."),
        book(606, "Can't Hurt Me", "David Goggins", "Jaico Publishing", 6, 499, 31, 4, 83, CRIMSON, SLATE,
            "A memoir of mental toughness: how a man built an unbreakable mind and a life of purpose.")
    )
}
