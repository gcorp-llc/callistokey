package com.example.ime.nlp

/**
 * Curated offline core vocabulary and common sentence/phrase patterns for English and Persian.
 * Optimized for zero-latency in-memory lookup and immediate out-of-the-box spell checking
 * and sentence prediction.
 */
object CoreLexicon {

    fun normalizePersian(text: String): String {
        return text
            .replace('\u064A', '\u06CC') // Arabic Yeh -> Persian Yeh
            .replace('\u0643', '\u06A9') // Arabic Kaf -> Persian Kaf
            .replace('\u0629', '\u0647') // Teh Marbuta -> Heh
    }

    /**
     * Core English words ordered roughly from highest to lower everyday frequency.
     */
    val englishWords: LinkedHashMap<String, Int> by lazy {
        val list = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
            "hello", "hi", "hey", "thanks", "thank", "please", "sorry", "yes", "yeah", "okay",
            "ok", "sure", "great", "awesome", "nice", "fine", "good", "morning", "afternoon", "evening",
            "night", "today", "tomorrow", "yesterday", "soon", "later", "always", "never", "maybe", "really",
            "very", "much", "more", "many", "need", "help", "send", "sent", "call", "message",
            "text", "email", "phone", "number", "address", "name", "home", "office", "meeting", "schedule",
            "project", "update", "check", "confirm", "ready", "done", "finished", "working", "waiting", "coming",
            "going", "leaving", "arrived", "here", "there", "where", "why", "what", "when", "which",
            "love", "happy", "glad", "hope", "wish", "best", "regards", "welcome", "friend", "family",
            "team", "group", "plan", "idea", "question", "answer", "problem", "issue", "fixed", "bug",
            "code", "keyboard", "android", "app", "mobile", "screen", "fast", "clean", "modern", "secure",
            "privacy", "offline", "data", "history", "clear", "delete", "save", "saved", "settings", "language",
            "persian", "english", "symbol", "number", "letter", "word", "sentence", "phrase", "spell", "correct",
            "suggest", "typing", "type", "write", "read", "open", "close", "start", "stop", "next",
            "previous", "back", "enter", "space", "shift", "search", "find", "share", "copy", "paste",
            "file", "folder", "image", "photo", "video", "audio", "music", "play", "pause", "watch",
            "book", "food", "water", "coffee", "tea", "lunch", "dinner", "breakfast", "car", "bus",
            "train", "flight", "travel", "trip", "city", "country", "world", "weather", "sunny", "rain",
            "cold", "hot", "warm", "big", "small", "long", "short", "high", "low", "right",
            "left", "top", "bottom", "inside", "outside", "before", "during", "while", "until", "since",
            "without", "within", "between", "around", "through", "across", "against", "under", "above", "below",
            "should", "must", "might", "could", "would", "shall", "being", "been", "having", "doing",
            "making", "going", "taking", "seeing", "looking", "getting", "trying", "asking", "telling", "thinking",
            "feeling", "wanted", "needed", "called", "asked", "told", "gave", "found", "thought", "knew",
            "became", "left", "kept", "let", "began", "seemed", "helped", "talked", "turned", "started",
            "showed", "heard", "played", "moved", "liked", "lived", "believed", "brought", "happened", "wrote",
            "provided", "sat", "stood", "lost", "paid", "met", "included", "continued", "set", "learned",
            "changed", "led", "understood", "watched", "followed", "stopped", "created", "spoke", "read", "allowed",
            "added", "spent", "grew", "opened", "walked", "won", "offered", "remembered", "considered", "appeared",
            "bought", "served", "sent", "expected", "built", "stayed", "fell", "cut", "reached", "remained",
            "amazing", "wonderful", "perfect", "excellent", "important", "possible", "different", "available", "special", "simple",
            "easy", "hard", "true", "false", "real", "full", "empty", "early", "late", "quick",
            "slow", "strong", "clear", "dark", "light", "bright", "quiet", "loud", "safe", "free",
            "are", "is", "was", "were", "am", "has", "had", "does", "did", "don't",
            "can't", "won't", "it's", "i'm", "you're", "we're", "they're", "that's", "there's", "what's"
        )
        LinkedHashMap<String, Int>(list.size * 2).apply {
            list.forEachIndexed { idx, w ->
                // Higher score for earlier words
                put(w, (list.size - idx) * 10 + 50)
            }
        }
    }

    /**
     * Core Persian words (with standard Persian Yeh 'ی' and Kaf 'ک', plus proper ZWNJ compounds).
     */
    val persianWords: LinkedHashMap<String, Int> by lazy {
        val list = listOf(
            "سلام", "خوبی", "خوبم", "ممنون", "مرسی", "تشکر", "لطفاً", "لطفا", "خواهش", "می‌کنم",
            "خداحافظ", "ببخشید", "معذرت", "متشکرم", "عالی", "خیلی", "بسیار", "خوب", "بد", "زیاد",
            "کم", "همه", "هیچ", "چند", "چطور", "چرا", "کجا", "کی", "چی", "چیه",
            "این", "آن", "اون", "من", "تو", "او", "ما", "شما", "آنها", "ایشان",
            "است", "هست", "نیست", "بود", "شد", "می‌شود", "نمی‌شود", "می‌شه", "نمی‌شه", "باشه",
            "باشید", "بله", "آره", "نه", "خیر", "حتماً", "حتما", "شاید", "البته", "واقعاً",
            "در", "به", "از", "با", "برای", "تا", "روی", "زیر", "پیش", "بعد",
            "قبل", "بین", "کنار", "داخل", "بیرون", "بالا", "پایین", "راست", "چپ", "جلو",
            "امروز", "فردا", "دیروز", "الان", "حالا", "زود", "دیر", "همیشه", "هرگز", "گاهی",
            "صبح", "ظهر", "عصر", "شب", "هفته", "ماه", "سال", "ساعت", "دقیقه", "ثانیه",
            "وقت", "بخیر", "خسته", "نباشید", "عزیز", "عزیزم", "دوست", "رفیق", "همکار", "خانواده",
            "کار", "پروژه", "برنامه", "جلسه", "گزارش", "پیام", "تماس", "شماره", "آدرس", "ایمیل",
            "فایل", "عکس", "فیلم", "صدا", "متن", "صفحه", "کیبورد", "گوشی", "موبایل", "کامپیوتر",
            "لپ‌تاپ", "اینترنت", "شبکه", "سرعت", "امنیت", "حریم", "خصوصی", "تنظیمات", "ذخیره", "حذف",
            "پاک", "جستجو", "ارسال", "دریافت", "تایید", "لغو", "شروع", "پایان", "ادامه", "توقف",
            "بیشتر", "کمتر", "بهتر", "بهترین", "جدید", "قدیمی", "بزرگ", "کوچک", "سریع", "آسان",
            "سخت", "مهم", "لازم", "مفید", "کامل", "درست", "غلط", "املایی", "اصلاح", "پیشنهاد",
            "جمله", "جملات", "کلمه", "کلمات", "حرف", "حروف", "زبان", "فارسی", "انگلیسی", "عدد",
            "اعداد", "علامت", "نماد", "فاصله", "نیم‌فاصله", "می‌خواهم", "نمی‌خواهم", "می‌خوام", "می‌توانم", "نمی‌توانم",
            "می‌تونم", "نمی‌تونم", "می‌دانم", "نمی‌دانم", "می‌دونم", "نمی‌دونم", "دارم", "داری", "دارد", "داریم",
            "دارید", "دارند", "ندارم", "ندارد", "خواهم", "خواهد", "باید", "نباید", "می‌گویم", "گفت",
            "می‌گوید", "می‌روم", "می‌رم", "می‌آیم", "می‌آم", "آمد", "رفت", "دید", "شنید", "نوشت",
            "خواند", "گرفت", "داد", "کرد", "می‌کند", "کردم", "کردی", "کرده", "شده", "بودم",
            "هستم", "هستی", "هستیم", "هستید", "هستند", "نیستم", "جان", "موفق", "باشی", "سلامت",
            "انشالله", "مبارک", "تولد", "عید", "سفر", "خانه", "خونه", "شرکت", "دفتر", "دانشگاه",
            "مدرسه", "کلاس", "کتاب", "درس", "امتحان", "نتیجه", "خبر", "اخبار", "وضعیت", "حالت",
            "چطوره", "چطوری", "قربونت", "فدات", "لطف", "محبت", "زحمت", "تشریف", "بفرمایید", "بفرما",
            "صبر", "کنید", "ببینم", "بگو", "بگید", "بنویس", "بفرست", "بگیر", "بده", "بررسی",
            "انجام", "آماده", "تموم", "تمام", "توضیح", "سوال", "جواب", "پاسخ", "مشکل", "راه‌حل",
            "ایده", "نظر", "پیشنهادات", "انتقاد", "کیفیت", "طراحی", "مدرن", "زیبا", "تمیز", "حرفه‌ای",
            "دقیق", "هوشمند", "دانشنامه", "یادگیری", "تاریخچه", "حافظه", "کاربر", "سیستم", "نسخه", "به‌روز",
            "تغییر", "تبدیل", "انتخاب", "فعال", "غیرفعال", "بله", "حتی", "فقط", "همین", "همان",
            "دیگر", "دیگه", "پس", "اگر", "اگه", "ولی", "اما", "چون", "زیرا", "بنابراین",
            "یا", "و", "که", "را", "هم", "نیز", "هر", "چیز", "کسی", "جایی",
            "ممنونم", "سپاسگزارم", "درود", "خوش", "آمدید", "مراقب", "خودت", "باش", "شب", "بخیر",
            "صبح", "بخیر", "عصر", "بخیر", "روز", "خوش", "سالم", "شاد", "پیروز", "سربلند",
            "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه", "یکشنبه", "تهران", "ایران", "جهان",
            "پول", "قیمت", "تومان", "ریال", "خرید", "فروش", "سفارش", "پرداخت", "حساب", "کارت",
            "بانک", "رمز", "عبور", "نام", "نام‌خانوادگی", "سن", "شهر", "خیابان", "کوچه", "پلاک",
            "غذا", "آب", "چای", "قهوه", "ناهار", "شام", "صبحانه", "میوه", "نان", "شیرینی"
        )
        LinkedHashMap<String, Int>(list.size * 2).apply {
            list.forEachIndexed { idx, w ->
                put(w, (list.size - idx) * 10 + 50)
            }
        }
    }

    /**
     * Curated English next-word and multi-word phrase continuations for instant sentence prediction.
     */
    val englishPhrases: Map<String, List<String>> = mapOf(
        "how" to listOf("are you?", "is it going?", "much", "about"),
        "are" to listOf("you", "you doing?", "we", "they"),
        "thank" to listOf("you", "you so much", "you very much", "you!"),
        "thanks" to listOf("a lot!", "for your help", "so much"),
        "hello" to listOf("everyone", "there!", "friend", "how are you?"),
        "hi" to listOf("there!", "how are you?", "everyone"),
        "good" to listOf("morning!", "afternoon", "evening", "night", "luck!", "job!"),
        "see" to listOf("you soon", "you tomorrow", "you later", "what"),
        "let" to listOf("me know", "us know", "me check"),
        "i" to listOf("am", "will", "have", "think", "need", "want"),
        "i'm" to listOf("on my way", "doing great", "sorry", "ready"),
        "on" to listOf("my way", "time", "the way"),
        "no" to listOf("problem!", "worries", "way"),
        "sounds" to listOf("good!", "great!", "like a plan"),
        "please" to listOf("check this", "let me know", "send me", "wait"),
        "have" to listOf("a great day!", "a good one", "you seen"),
        "what" to listOf("do you think?", "is up?", "time"),
        "can" to listOf("you help me?", "we meet", "you send"),
        "will" to listOf("do!", "check it", "be there soon")
    )

    /**
     * Curated Persian next-word and multi-word phrase continuations for instant sentence prediction.
     */
    val persianPhrases: Map<String, List<String>> = mapOf(
        "سلام" to listOf("خوبی؟", "عزیزم", "وقت بخیر", "دوست من"),
        "سلام،" to listOf("خوبی؟", "وقت بخیر", "چطوری؟"),
        "درود" to listOf("بر شما", "دوست عزیز", "وقت بخیر"),
        "خوبی" to listOf("چه خبر؟", "شکر خدا", "همه چی خوبه؟"),
        "ممنون" to listOf("از شما", "عزیزم", "لطف کردید", "بابت کمک"),
        "تشکر" to listOf("از شما", "فراوان", "بابت پیگیری"),
        "سپاسگزارم" to listOf("از لطف شما", "عزیز", "موفق باشید"),
        "خسته" to listOf("نباشید", "نباشی", "شدم"),
        "وقت" to listOf("بخیر", "شما بخیر", "دارید؟"),
        "صبح" to listOf("بخیر", "شما بخیر", "زود"),
        "شب" to listOf("بخیر", "خوش", "آرام"),
        "عصر" to listOf("بخیر", "شما بخیر"),
        "خواهش" to listOf("می‌کنم", "می‌کنم، قابلی نداشت"),
        "لطفاً" to listOf("بررسی کنید", "ارسال کنید", "اطلاع دهید", "صبر کنید"),
        "لطفا" to listOf("بررسی کنید", "ارسال کنید", "اطلاع دهید", "صبر کنید"),
        "در" to listOf("اسرع وقت", "حال حاضر", "مورد", "خصوص"),
        "با" to listOf("تشکر", "احترام", "کمال میل", "سلام"),
        "چه" to listOf("خبر؟", "حالی؟", "جالب", "زمانی؟"),
        "من" to listOf("خوبم ممنون", "الان می‌فرستم", "آماده‌ام", "در خدمتم"),
        "ببخشید" to listOf("مزاحم شدم", "متوجه نشدم", "الان می‌بینم"),
        "مراقب" to listOf("خودت باش", "خودتون باشید"),
        "موفق" to listOf("باشید", "باشی", "و پیروز باشید"),
        "انشالله" to listOf("به زودی", "همه چی درست میشه", "موفق باشید"),
        "الان" to listOf("می‌فرستم", "بررسی می‌کنم", "در دسترس نیستم", "آماده میشه"),
        "خیلی" to listOf("خوب", "عالی", "ممنون", "زیبا")
    )

    val englishSentenceStarters = listOf("Hello", "How are you?", "Thanks", "I am", "Please")
    val persianSentenceStarters = listOf("سلام", "خسته نباشید", "ممنون از شما", "وقت بخیر", "لطفاً")
}
