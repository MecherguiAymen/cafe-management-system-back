package com.mechergui.cafe.POJO;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.*;
import java.io.Serializable;

@NamedQuery(name = "Bill.getAllBills",query = "select b from Bill b order by b.id desc")

@NamedQuery(name = "Bill.getBillsByUserName",query = "select b from Bill b  where b.createdBy=:username order by b.id desc")


@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "bill")
public class Bill implements Serializable {

    private static final long serialVersionUID=1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "contactnumber")
    private String contactNumber;

    @Column(name = "payementmethod")
    private String payementMethod;

    @Column(name = "total")
    private Integer total;


    @Column(name = "productdetails",columnDefinition = "json")
    private String productDetails;

    @Column(name = "createdby")
    private String createdBy;

}

