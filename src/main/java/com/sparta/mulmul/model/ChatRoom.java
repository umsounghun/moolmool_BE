package com.sparta.mulmul.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import javax.persistence.*;

@Getter @Entity
@NoArgsConstructor
public class ChatRoom extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    private Long id;

    // PK만 넣지 말고 관련 정보들이 같이 넣어 두는 게 어떨까요?
    @ManyToOne
    @JoinColumn(nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User acceptor;

    @Column(nullable = false)
    private Boolean reqOut;

    @Column(nullable = false)
    private Boolean accOut;

    @Column(nullable = false)
    private Boolean accFixed = false;

    @Column(nullable = false)
    private Boolean reqFixed = false;

    public static ChatRoom createOf(User requester, User acceptor){

        ChatRoom room = new ChatRoom();

        room.requester = requester;
        room.acceptor = acceptor;
        room.reqOut = false;
        room.accOut = true;

        return room;
    }

    public void reqOut(Boolean bool) { this.reqOut = bool; }
    public void accOut(Boolean bool) { this.accOut = bool; }

    public void fixedRoom(String flag) {
        if (flag.equals("acceptor")) {
            this.accFixed = true;
        } else {
            this.reqFixed = true;
        }
    }
}
