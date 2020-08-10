/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "cris_layout_field")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutField implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_layout_field_field_id_seq")
    @SequenceGenerator(
        name = "cris_layout_field_field_id_seq",
        sequenceName = "cris_layout_field_field_id_seq",
        allocationSize = 1)
    @Column(name = "field_id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_field_id")
    private MetadataField metadataField;
    @Column(name = "bundle")
    private String bundle;
    @Column(name = "rendering")
    private String rendering;
    @Column(name = "row", nullable = false)
    private Integer row;
    @Column(name = "type")
    private String type;
    @Column(name = "label")
    private String label;
    @Column(name = "style")
    private String style;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "layoutField")
    private Set<CrisLayoutFieldBitstream> bitstreams;
    @OneToMany(
        mappedBy = "field",
        cascade = CascadeType.REMOVE
    )
    private List<CrisLayoutBox2Field> box2field = new ArrayList<>();

    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MetadataField getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getRendering() {
        return rendering;
    }

    public void setRendering(String rendering) {
        this.rendering = rendering;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Set<CrisLayoutFieldBitstream> getBitstreams() {
        return bitstreams;
    }

    public void setBitstreams(Set<CrisLayoutFieldBitstream> bitstreams) {
        this.bitstreams = bitstreams;
    }

    public List<CrisLayoutBox2Field> getBox2field() {
        return box2field;
    }

    public void setBox2field(List<CrisLayoutBox2Field> box2field) {
        this.box2field = box2field;
    }

}