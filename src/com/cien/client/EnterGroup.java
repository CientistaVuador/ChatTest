/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cien.client;

/**
 *
 * @author Cien
 */
public class EnterGroup extends javax.swing.JFrame {

    /**
     * Creates new form EnterGroup
     */
    public EnterGroup() {
        initComponents();
        start();
    }
    
    private void start() {
        setLocationRelativeTo(null);
    }
        

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        groupNameField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        groupPassField = new javax.swing.JPasswordField();
        enterGroupButton = new javax.swing.JButton();
        errorLabel = new javax.swing.JLabel();

        setTitle("Entrar");
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);

        jLabel1.setText("Nome do Grupo");

        jLabel2.setText("Senha");

        enterGroupButton.setText("Entrar no Grupo");

        errorLabel.setForeground(new java.awt.Color(255, 0, 0));
        errorLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(groupPassField)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addGap(0, 128, Short.MAX_VALUE))
                    .addComponent(groupNameField)
                    .addComponent(enterGroupButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(errorLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(groupNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(groupPassField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(errorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enterGroupButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton enterGroupButton;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JTextField groupNameField;
    private javax.swing.JPasswordField groupPassField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the enterGroupButton
     */
    public javax.swing.JButton getEnterGroupButton() {
        return enterGroupButton;
    }

    /**
     * @return the errorLabel
     */
    public javax.swing.JLabel getErrorLabel() {
        return errorLabel;
    }

    /**
     * @return the groupNameField
     */
    public javax.swing.JTextField getGroupNameField() {
        return groupNameField;
    }

    /**
     * @return the groupPassField
     */
    public javax.swing.JPasswordField getGroupPassField() {
        return groupPassField;
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (!b) {
            groupNameField.setText("");
            groupPassField.setText("");
            errorLabel.setText("");
        }
    }
    
}
