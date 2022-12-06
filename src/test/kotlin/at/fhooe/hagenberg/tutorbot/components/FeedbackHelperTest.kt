package at.fhooe.hagenberg.tutorbot.components

import at.fhooe.hagenberg.tutorbot.components.FeedbackHelper.FeedbackCount
import at.fhooe.hagenberg.tutorbot.testutil.CommandLineTest
import at.fhooe.hagenberg.tutorbot.testutil.assertThrows
import at.fhooe.hagenberg.tutorbot.testutil.getResource
import at.fhooe.hagenberg.tutorbot.testutil.rules.FileSystemRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class FeedbackHelperTest : CommandLineTest() {
    private val lowerCaseStudentNumbers = listOf("s1", "s2", "s3", "s4", "s2210101010")
    private val studentFeedbackCount = listOf(
        "s1" to FeedbackCount(2, 0),
        "s2" to FeedbackCount(0, 2),
        "s3" to FeedbackCount(1, 0),
        "s4" to FeedbackCount(1, 1),
        "s2210101010" to FeedbackCount(0, 1)
    )

    private val validCsv = mapOf(
        "s1" to FeedbackCount(1, 3),
        "s2" to FeedbackCount(3, 3),
        "s4" to FeedbackCount(0, 1)
    )

    private val reviewDir = File(ClassLoader.getSystemResource("pdfs").toString().split("file:/").last())

    private val feedbackHelper = FeedbackHelper()

    @get:Rule
    val fileSystem = FileSystemRule()

    @Test
    fun `Read reviews ignores invalid files`() {
        val res = feedbackHelper.readAllReviewsFromDir(reviewDir)
        val invalidFile1 = "review.pdf"
        val invalidFile2 = "s1-invalid-file.pdf"

        assert(res.none { r -> r.fileName.contains(invalidFile1) || r.fileName.contains(invalidFile2) })
    }

    @Test
    fun `Read reviews returns empty when dir empty`() {
        val emptyDir = File(fileSystem.directory.absolutePath)
        val res = feedbackHelper.readAllReviewsFromDir(emptyDir)

        assert(res.isEmpty())
    }

    @Test
    fun `Read reviews includes right amount of files`() {
        val res = feedbackHelper.readAllReviewsFromDir(reviewDir)

        assert(res.size == 4)
    }

    @Test
    fun `Read reviews gets student numbers lower case`() {
        val res = feedbackHelper.readAllReviewsFromDir(reviewDir)

        assert(res.all { r -> r.revStudentNr in lowerCaseStudentNumbers && r.subStudentNr in lowerCaseStudentNumbers })
    }

    @Test
    fun `Read feedback returns empty when dir empty`() {
        val emptyDir = File(fileSystem.directory.absolutePath)
        val res = feedbackHelper.readFeedbackCountFromReviews(emptyDir)

        assert(res.isEmpty())
    }

    @Test
    fun `Read feedback has key for every lower case student number`() {
        val res = feedbackHelper.readFeedbackCountFromReviews(reviewDir)

        assert(res.all { f -> f.key in lowerCaseStudentNumbers })
    }

    @Test
    fun `Read feedback FeedbackCount has correct amount of submissions and reviews for each student`() {
        val res = feedbackHelper.readFeedbackCountFromReviews(reviewDir)

        assert(studentFeedbackCount.all { s -> res[s.first] == s.second })
    }

    @Test
    fun `Read feedback from csv throws exception when file empty`(){
        val file = fileSystem.directory.resolve("invalid-empty.csv")

        assertThrows<IOException> {
            feedbackHelper.readFeedbackCountFromCsv(file)
        }
    }

    @Test
    fun `Read feedback from csv throws exception when file does not exist`(){
        val file = fileSystem.directory.resolve("notexists.csv")

        assertThrows<FileNotFoundException> {
            feedbackHelper.readFeedbackCountFromCsv(file)
        }
    }

    @Test
    fun `Read feedback from csv throws exception when header not present`(){
        val file = getResource("csv/invalid-noheader.csv")

        assertThrows<IllegalArgumentException> {
            feedbackHelper.readFeedbackCountFromCsv(file)
        }
    }

    @Test
    fun `Read feedback from csv throws exception when count not int`(){
        val file = getResource("csv/invalid-count.csv")

        assertThrows<NumberFormatException> {
            feedbackHelper.readFeedbackCountFromCsv(file)
        }
    }

    @Test
    fun `Read feedback from valid csv returns same values`(){
        val file = getResource("csv/valid.csv")
        val res = feedbackHelper.readFeedbackCountFromCsv(file)

        assertEquals(validCsv, res)
    }

    @Test
    fun `Read write read feedback returns same values`(){
        val file = getResource("csv/valid.csv")
        val newFile = fileSystem.directory.resolve("newfile.csv")

        val expected = feedbackHelper.readFeedbackCountFromCsv(file)
        feedbackHelper.writeFeedbackCountToCsv(newFile, expected)
        val res = feedbackHelper.readFeedbackCountFromCsv(newFile)

        assertEquals(expected, res)
    }
}