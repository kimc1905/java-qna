package codesquad.domain;

import codesquad.CannotDeleteException;
import codesquad.UnAuthorizedException;
import codesquad.dto.QuestionDto;
import org.hibernate.annotations.Where;
import support.domain.AbstractEntity;
import support.domain.UrlGeneratable;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class Question extends AbstractEntity implements UrlGeneratable, Content {
    @Size(min = 3, max = 100)
    @Column(length = 100, nullable = false)
    private String title;

    @Size(min = 3)
    @Lob
    private String contents;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_question_writer"))
    private User writer;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @Where(clause = "deleted = false")
    @OrderBy("id ASC")
    private List<Answer> answers = new ArrayList<>();

    private boolean deleted = false;

    public Question() {
    }

    public Question(String title, String contents) {
        this.title = title;
        this.contents = contents;
    }

    public Question(String title, String contents, User writer) {
        this.title = title;
        this.contents = contents;
        this.writer = writer;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public long getContentId() {
        return getId();
    }

    public String getContents() {
        return contents;
    }

    public User getWriter() {
        return writer;
    }

    public void writeBy(User loginUser) {
        this.writer = loginUser;
    }

    public void update(User loginUser, Question updatedQuestion) {
        if(!isOwner(loginUser))
            throw new UnAuthorizedException();
        this.title = updatedQuestion.title;
        this.contents = updatedQuestion.contents;
    }

    public void delete(User loginUser) throws CannotDeleteException {
        if(!isOwner(loginUser))
            throw new CannotDeleteException("Question must be deleted by owner.");
        deleted = true;
        deleteAnswer(loginUser);
    }

    public void addAnswer(Answer answer) {
        answer.toQuestion(this);
        answers.add(answer);
    }

    private void deleteAnswer(User loginUser) throws CannotDeleteException {
        for (Answer answer : answers) {
            if(!answer.isDeleted())
                answer.delete(loginUser);
        }
    }

    public boolean isOwner(User loginUser) {
        return writer.equals(loginUser);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getAnswerCount() {
        return (int)answers.stream().filter(answer -> !answer.isDeleted()).count();
    }

    public List<Answer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }

    @Override
    public ContentType getContentType() {
        return ContentType.QUESTION;
    }

    @Override
    public String generateUrl() {
        return String.format("/questions/%d", getId());
    }

    public QuestionDto toQuestionDto() {
        return new QuestionDto(getId(), this.title, this.contents);
    }

    @Override
    public String toString() {
        return "Question [id=" + getId() + ", title=" + title + ", contents=" + contents + ", writer=" + writer + "]";
    }

}
